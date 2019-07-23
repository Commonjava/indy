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
import org.commonjava.indy.client.core.helper.HttpResources;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RetrievedContentMatchesContentLength_SlowClient_Test
                extends AbstractHttproxFunctionalTest
{

    // NOTE: anything less, and it seems like this test doesn't check for stream truncation.
    private static final int CONTENT_LENGTH = 1024 * 384;

    private static int SELECTION_OFFSET = 32; // let's getOrCreate to printable ASCII characters

    private static int SELECTION_RANGE_SIZE = 126 - SELECTION_OFFSET;
                    // from the ASCII table...let's just keep it simple

    private static final String USER = "user";

    private static final String PASS = "password";

    private Random rand = new Random();

    @Test
    public void run()
                    throws Exception
    {
        final String testRepo = "test";
        String path = "path/to/binary.bin";
        StringBuilder content = new StringBuilder();
        for ( int i = 0; i < CONTENT_LENGTH; i++ )
        {
            content.append( (char) rand.nextInt( SELECTION_RANGE_SIZE ) + SELECTION_OFFSET );
        }

        final String url = server.formatUrl( testRepo, path );
        server.expect( url, 200, content.toString() );

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            assertThat( response.getStatusLine().getStatusCode(), equalTo( 200 ) );

            stream = response.getEntity().getContent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buf = new byte[1024];
            int read = -1;
            while ( ( read = stream.read( buf ) ) > -1 )
            {
                baos.write( buf, 0, read );
            }

            final String resultingPom = new String(baos.toByteArray());

            assertThat( resultingPom, notNullValue() );
            assertThat( resultingPom, equalTo( content.toString() ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpResources.cleanupResources( get, response, client );
        }
    }

    @Override
    protected String getAdditionalHttproxConfig()
    {
        return "tracking.type=always";
    }

    @Override
    protected int getTestTimeoutMultiplier()
    {
        return 2;
    }
}
