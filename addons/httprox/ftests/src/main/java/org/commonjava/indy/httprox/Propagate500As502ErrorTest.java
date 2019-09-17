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

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Ignore;
import org.junit.Test;

@Ignore( "passing 5xx upstream errors through the repo manager as 404 to support maven" )
public class Propagate500As502ErrorTest
    extends AbstractHttproxFunctionalTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    @Test
    public void run()
        throws Exception
    {
        final String testRepo = "test";
        final String url = server.formatUrl( testRepo, "org/test/foo/1/foo-1.pom" );
        server.registerException( url, "Expected error", HttpStatus.SC_INTERNAL_SERVER_ERROR );

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        final InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            assertThat( response.getStatusLine()
                                .getStatusCode(), equalTo( HttpStatus.SC_BAD_GATEWAY ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpResources.cleanupResources( get, response, client );
        }

        final RemoteRepository remoteRepo = this.client.stores()
                       .load( new StoreKey( GENERIC_PKG_KEY, StoreType.remote, "httprox_127-0-0-1"), RemoteRepository.class );

        assertThat( remoteRepo, notNullValue() );
        assertThat( remoteRepo.getUrl(), equalTo( server.getBaseUri() ) );
    }

}
