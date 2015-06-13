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
package org.commonjava.aprox.test.fixture.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

public class HttpTestFixture
    extends ExternalResource
{

    public TemporaryFolder folder = new TemporaryFolder();

    public TestHttpServer server;

    private final AproxHttpProvider http;

    public HttpTestFixture( final String baseResource )
    {
        server = new TestHttpServer( baseResource );

        try
        {
            folder.create();
        }
        catch ( final IOException e )
        {
            throw new RuntimeException( "Failed to setup temp folder.", e );
        }

        http = new AproxHttpProvider( new MemoryPasswordManager() );
        http.setup();
    }

    @Override
    protected void after()
    {
        server.after();
        folder.delete();
        super.after();
    }

    @Override
    protected void before()
        throws Throwable
    {
        super.before();
        server.before();
    }

    public TemporaryFolder getFolder()
    {
        return folder;
    }

    public TestHttpServer getServer()
    {
        return server;
    }

    public AproxHttpProvider getHttp()
    {
        return http;
    }

    public File newFile( final String fileName )
        throws IOException
    {
        return folder.newFile( fileName );
    }

    public File newFile()
        throws IOException
    {
        return folder.newFile();
    }

    public File newFolder( final String... folderNames )
        throws IOException
    {
        return folder.newFolder( folderNames );
    }

    public File newFolder()
        throws IOException
    {
        return folder.newFolder();
    }

    public File getRoot()
    {
        return folder.getRoot();
    }

    public int getPort()
    {
        return server.getPort();
    }

    public Map<String, Integer> getAccessesByPath()
    {
        return server.getAccessesByPath();
    }

    public String formatUrl( final String... subpath )
    {
        return server.formatUrl( subpath );
    }

    public String getBaseUri()
    {
        return server.getBaseUri();
    }

    public String getUrlPath( final String url )
        throws MalformedURLException
    {
        return server.getUrlPath( url );
    }

    public void registerException( final String url, final String error )
    {
        server.registerException( "GET", url, error );
    }

    public void registerException( final String method, final String url, final String error )
    {
        server.registerException( method, url, error );
    }

    public Map<String, ContentResponse> getRegisteredErrors()
    {
        return server.getRegisteredErrors();
    }

    public void get( final String testUrl, final int expectedResponse )
        throws Exception
    {
        final HttpGet get = new HttpGet( testUrl );
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try
        {
            client = http.createClient();
            response = client.execute( get, http.createContext() );

            final StatusLine sl = response.getStatusLine();

            assertThat( sl.getStatusCode(), equalTo( expectedResponse ) );
        }
        finally
        {
            http.cleanup( client, get, response );
        }
    }

    public void expect( final String testUrl, final int responseCode, final String body )
        throws Exception
    {
        server.expect( "HEAD", testUrl, responseCode, body );
        server.expect( "GET", testUrl, responseCode, body );
    }

    public void expect( final String testUrl, final int responseCode )
        throws Exception
    {
        server.expect( "HEAD", testUrl, responseCode, (String) null );
        server.expect( "GET", testUrl, responseCode, (String) null );
    }

    public void expect( final String method, final String testUrl, final int responseCode, final String body )
        throws Exception
    {
        server.expect( testUrl, responseCode, body );
    }

    public void expect( final String method, final String testUrl, final int responseCode )
        throws Exception
    {
        server.expect( method, testUrl, responseCode, (String) null );
    }

}
