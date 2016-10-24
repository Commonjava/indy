/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Collections;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.commonjava.indy.koji.content.KojiContentManagerDecorator.KOJI_ORIGIN;

public class ProxyRemoteKojiContentTest
{
    final static Logger logger = LoggerFactory.getLogger( ProxyRemoteKojiContentTest.class );

    /**
     * Accessing an existing external Koji site and retrieve artifact. This is disabled by default. In order to run it,
     * you need an external Koji instance and start Indy via:
     * 1. export TEST_ETC=<your-koji-config-dir> => with proper settings for Koji url, pem, etc
     * 2. bin/test-setup.sh => to start Indy
     * 3. run this test
     */
    @Ignore
    @Test
    public void proxyRemoteKojiArtifact()
        throws Exception
    {
        final String path = "org/dashbuilder/dashbuilder-all/0.4.0.Final-redhat-10/dashbuilder-all-0.4.0.Final-redhat-10.pom";
        Indy client = new Indy( "http://localhost:8080/api", new IndyObjectMapper( Collections.emptySet() ),
                Collections.emptySet() ).connect();

        // would be slow the first time to get an artifact
        long elapse = testGet( client, path );
        logger.debug("Get (first) use " + elapse + " milliseconds");

        // the following get should have been cached and fast
        elapse = testGet( client, path );
        logger.debug("Get (second) use " + elapse + " milliseconds");

        // the remote store should have been added to public group
        StoreListingDTO<RemoteRepository> repos = client.stores().listRemoteRepositories();
        for ( RemoteRepository repo : repos.getItems() ) {
            logger.debug("Repo " + repo.getName());
            if (repo.getName().startsWith(KOJI_ORIGIN)) {
                logger.debug("Koji repo patterns: " + repo.getPathMaskPatterns());
            }
        }

    }

    private long testGet( Indy client, String path )
            throws Exception
    {
        long t1 = System.currentTimeMillis();
        logger.debug("Start getting... (" + t1 + ")");
        try (InputStream stream = client.content().get(group, "public", path)) {
            assertThat(stream, notNullValue());
        }
        long t2 = System.currentTimeMillis();
        logger.debug("Finish getting... (" + t2 + ")");
        return t2 - t1;
    }

}
