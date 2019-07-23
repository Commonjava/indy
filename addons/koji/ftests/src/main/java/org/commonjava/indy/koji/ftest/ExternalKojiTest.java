/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.koji.ftest;

import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.koji.client.IndyKojiClientModule;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.commonjava.indy.koji.model.IndyKojiConstants.KOJI_ORIGIN;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Accessing an existing external Koji site and retrieve artifact. All external tests are disabled by default.
 *
 * In order to run it, you need an external Koji instance and start Indy via:
 *
 * 1. export TEST_ETC={indy-etc-dir}, with proper settings for Koji url, pem, etc
 * 2. $INDY_HOME/bin/test-setup.sh, to start Indy
 * 3. look at TEST_ETC/conf.d/koji.conf for something like, target.build.+=brew-proxies
 *    make sure BOTH the pseudo group (e.g., group-1) and target group (e.g., brew-proxies) are ready in Indy.
 * 4. run external tests
 *
 * Created by ruhan on 3/27/18.
 */
public class ExternalKojiTest
{
    final Logger logger = LoggerFactory.getLogger( getClass() );

    protected Indy client;

    /* If we have (in koji.conf) target.build.+=brew-proxies,
     * when accessing files in this pseudo group, the proxy-ed koji repositories will be added to group "brew-proxies";
     */
    protected static String pseudoGroupName = "build-1";

    protected static String kojiProxyGroupName = "brew-proxies";

    @Before
    public void setupExternalKojiBase() throws IndyClientException
    {
        client = new Indy( "http://localhost:8080/api", new IndyObjectMapper( Collections.emptySet() ),
                           getAdditionalClientModules() ).connect();

        Group proxyGroup = new Group( "maven", kojiProxyGroupName );
        boolean exist = client.stores().exists( proxyGroup.getKey() );
        if ( !exist )
        {
            client.stores().create( proxyGroup, "adding brew-proxies", Group.class );
        }

        Group pseudoGroup = new Group( "maven", pseudoGroupName );
        exist = client.stores().exists( pseudoGroup.getKey() );
        if ( !exist )
        {
            client.stores().create( pseudoGroup, "adding pseudo group", Group.class );
        }
    }

    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.<IndyClientModule>asList( new IndyKojiClientModule() );
    }

    /**
     * Measure content download time.
     * @param groupName
     * @param path
     * @return
     * @throws Exception
     */
    protected long contentDownloadTime( String groupName, String path ) throws Exception
    {
        long t1 = System.currentTimeMillis();
        try (InputStream stream = client.content().get( group, groupName, path ))
        {
            assertThat( stream, notNullValue() );
        }
        long t2 = System.currentTimeMillis();
        return t2 - t1;
    }

    /**
     * Get all proxied koji remote repositories.
     * @return
     * @throws IndyClientException
     */
    protected List<RemoteRepository> getKojiRemoteRepositories() throws IndyClientException
    {
        List<RemoteRepository> ret = new ArrayList<>();

        StoreListingDTO<RemoteRepository> repos = client.stores().listRemoteRepositories();
        for ( RemoteRepository repo : repos.getItems() )
        {
            if ( repo.getName().startsWith( KOJI_ORIGIN ) )
            {
                logger.debug( "Koji repo patterns: " + repo.getPathMaskPatterns() );
                ret.add( repo );
            }
        }
        return ret;
    }
}
