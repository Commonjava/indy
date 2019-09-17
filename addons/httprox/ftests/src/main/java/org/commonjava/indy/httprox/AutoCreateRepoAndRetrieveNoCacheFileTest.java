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
import org.commonjava.indy.model.core.GenericPackageTypeDescriptor;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class AutoCreateRepoAndRetrieveNoCacheFileTest
    extends AbstractHttproxFunctionalTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    @Test
    public void run()
        throws Exception
    {
        final String path = "org/foo/bar/1.0/bar-1.0.nocache";
        final String content = "This is a test: " + System.nanoTime();

        final String testRepo = "test";

        final String url = server.formatUrl( testRepo, path );

        server.expect( url, 200, content );

        final HttpGet get = new HttpGet( url );
        CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            stream = response.getEntity().getContent();
            final String resultingPom = IOUtils.toString( stream );

            assertThat( resultingPom, notNullValue() );
            assertThat( resultingPom, equalTo( content ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpResources.cleanupResources( get, response, client );
        }

        final RemoteRepository remoteRepo = this.client.stores()
                                                       .load( new StoreKey( GENERIC_PKG_KEY, StoreType.remote,
                                                                            "httprox_127-0-0-1_" + server.getPort() ),
                                                              RemoteRepository.class );

        assertThat( remoteRepo, notNullValue() );
        assertThat( remoteRepo.getUrl(), equalTo( server.getBaseUri() ) );

        String pomUrl = this.client.content().contentUrl( remoteRepo.getKey(), testRepo, path ) + "?cache-only=true";
        System.out.println("pomUrl:: " + pomUrl);

        HttpHead head = new HttpHead( pomUrl );
        client = HttpClients.createDefault();

        try
        {
            response = client.execute( head );
            assertThat( response.getStatusLine().getStatusCode(), equalTo( 404 ) );
        }
        finally
        {
            HttpResources.cleanupResources( head, response, client );
        }

    }

    @Override
    protected int getTestTimeoutMultiplier()
    {
        return 1;
    }

    @Override
    protected String getAdditionalHttproxConfig()
    {
        return "nocache.patterns=.+nocache";
    }

}
