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
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SuccessiveRetrievalWithRemoteRepoDeletionBetweenTest
    extends AbstractHttproxFunctionalTest
{

    private static final String REMOTE_NAME = "httprox_127-0-0-1";

    @Test
    public void run()
        throws Exception
    {
        final String testRepo = "test";
        final PomRef pom = loadPom( "simple.pom", Collections.<String, String> emptyMap() );
        final String url = server.formatUrl( testRepo, pom.path );
        server.expect( url, 200, pom.pom );

        Stream.of( 1, 2 ).forEach( ( currentTry)->{
            CloseableHttpResponse response = null;
            InputStream stream = null;
            final HttpGet get = new HttpGet( url );
            CloseableHttpClient httpClient = null;
            try
            {
                httpClient = proxiedHttp();

                response = httpClient.execute( get );
                stream = response.getEntity()
                                 .getContent();
                final String resultingPom = IOUtils.toString( stream );

                assertThat( currentTry + ": retrieved POM was null!", resultingPom, notNullValue() );
                assertThat( currentTry + ": retrieved POM contains wrong content!", resultingPom, equalTo( pom.pom ) );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                fail( currentTry + ": Failed to retrieve file: " + url );
            }
            finally
            {
                IOUtils.closeQuietly( stream );
                HttpResources.cleanupResources( get, response, httpClient );
            }

            try
            {
                client.stores()
                      .delete( new StoreKey( GENERIC_PKG_KEY, StoreType.remote, REMOTE_NAME ),
                               "Deleting for pass: " + currentTry );
            }
            catch ( IndyClientException e )
            {
                fail( "Failed to delete remote repo: " + REMOTE_NAME );
            }
        } );

        final RemoteRepository remoteRepo = this.client.stores()
                                                       .load( new StoreKey( GENERIC_PKG_KEY, StoreType.remote,
                                                                            REMOTE_NAME ), RemoteRepository.class );

        assertThat( remoteRepo, nullValue() );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.<IndyClientModule> singleton( new IndyFoloAdminClientModule() );
    }

    @Override
    protected String getAdditionalHttproxConfig()
    {
        return "secured=false";
    }

}
