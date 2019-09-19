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
package org.commonjava.indy.httprox;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.httprox.handler.AbstractProxyRepositoryCreator;
import org.commonjava.indy.httprox.handler.ProxyCreationResult;
import org.commonjava.indy.httprox.handler.ProxyRepositoryCreator;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.http.util.UserPass;
import org.commonjava.indy.util.UrlInfo;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class AutoCreateRepoWithTrackingIdTest
                extends AbstractHttproxTrackingFunctionalTest
{

    private static final String TRACKING_ID = "A8DinNReIBj9NH";

    private static final String USER = TRACKING_ID + "+tracking";

    private static final String PASS = "password";

    private static final ProxyRepositoryCreator creator = new AbstractProxyRepositoryCreator()
    {
        @Override
        public ProxyCreationResult create( String trackingID, String name, String baseUrl, UrlInfo urlInfo,
                                           UserPass userPass, Logger logger )
        {
            return null;
        }
    };

    @Test
    public void run() throws Exception
    {
        final String testRepo = "test";
        final PomRef pom = loadPom( "simple.pom", Collections.<String, String>emptyMap() );
        final String url = server.formatUrl( testRepo, pom.path );
        server.expect( url, 200, pom.pom );

        final HttpGet get = new HttpGet( url );
        CloseableHttpClient client = proxiedHttp( USER, PASS );
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            stream = response.getEntity().getContent();
            final String resultingPom = IOUtils.toString( stream );

            assertThat( resultingPom, notNullValue() );
            assertThat( resultingPom, equalTo( pom.pom ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpResources.cleanupResources( get, response, client );
        }

        // Remote
        String remote = creator.formatId( HOST, 0, 0, TRACKING_ID, StoreType.remote );
        final RemoteRepository remoteRepo = this.client.stores()
                                                       .load( new StoreKey( GENERIC_PKG_KEY, StoreType.remote, remote ),
                                                              RemoteRepository.class );

        assertThat( remoteRepo, notNullValue() );
        assertThat( remoteRepo.getUrl(), equalTo( server.getBaseUri() ) );
        assertThat( remoteRepo.isPassthrough(), equalTo( false ) );

        String pomUrl = this.client.content().contentUrl( remoteRepo.getKey(), testRepo, pom.path )
                        + "?cache-only=true";
        HttpHead head = new HttpHead( pomUrl );
        client = HttpClients.createDefault();

        try
        {
            response = client.execute( head );

            assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );
        }
        finally
        {
            HttpResources.cleanupResources( head, response, client );
        }

        // Hosted
        String hosted = creator.formatId( HOST, 0, 0, TRACKING_ID, StoreType.hosted );
        final HostedRepository hostedRepo = this.client.stores()
                                                       .load( new StoreKey( GENERIC_PKG_KEY, StoreType.hosted, hosted ),
                                                              HostedRepository.class );

        assertThat( hostedRepo, notNullValue() );

        // Group
        final Group group = this.client.stores()
                                       .load( new StoreKey( GENERIC_PKG_KEY, StoreType.group,
                                                            creator.formatId( HOST, 0, 0, TRACKING_ID,
                                                                              StoreType.group ) ), Group.class );

        assertThat( group, notNullValue() );

        List<StoreKey> constituents = group.getConstituents();

        assertThat( constituents.contains( remoteRepo.getKey() ), equalTo( true ) );
        assertThat( constituents.contains( hostedRepo.getKey() ), equalTo( true ) );

    }

}
