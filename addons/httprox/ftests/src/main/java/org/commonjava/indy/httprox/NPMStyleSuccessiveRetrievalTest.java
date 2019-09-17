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
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class NPMStyleSuccessiveRetrievalTest
    extends AbstractHttproxFunctionalTest
{

    private static final String REMOTE_NAME = "httprox_127-0-0-1";

    @Test
    public void run()
        throws Exception
    {
        final String testRepo = "test";
        String pkgPath = "test-me";
        String tgzPath = "test-me/-/test-me-1.0.0.tgz";

        final InputStream is = Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream( "npm/test-me" );

        byte[] pkg = IOUtils.toByteArray( is );

        byte[] tgz = new byte[32];
        new Random().nextBytes( tgz );

        final String pkgUrl = server.formatUrl( testRepo, pkgPath );
        final String tgzUrl = server.formatUrl( testRepo, tgzPath );

        Map<String, byte[]> contentMap = new LinkedHashMap<>();
        contentMap.put( pkgUrl, pkg );
        contentMap.put( tgzUrl, tgz );

        server.expect( pkgUrl, 200, new ByteArrayInputStream( pkg ) );
        server.expect( tgzUrl, 200, new ByteArrayInputStream( tgz ) );

        contentMap.forEach( (url, expect)->{
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

                byte[] content = IOUtils.toByteArray( stream );

                assertThat( url+ ": content was null!", content, notNullValue() );
                assertThat( url + ": retrieved content was wrong!", content, equalTo( expect ) );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                fail( url + ": Failed to retrieve file. Reason: " + e.getMessage() );
            }
            finally
            {
                IOUtils.closeQuietly( stream );
                HttpResources.cleanupResources( get, response, httpClient );
            }
        } );
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
