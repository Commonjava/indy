/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.autoprox.fixture;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
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
        server.registerException( url, error );
    }

    public Map<String, String> getRegisteredErrors()
    {
        return server.getRegisteredErrors();
    }

    public void get( final String testUrl, final int expectedResponse )
        throws Exception
    {
        final HttpGet get = new HttpGet( testUrl );
        try
        {
            final HttpResponse response = http.getClient()
                                              .execute( get );

            final StatusLine sl = response.getStatusLine();

            assertThat( sl.getStatusCode(), equalTo( expectedResponse ) );
        }
        finally
        {
            get.reset();
        }
    }

    public void expect( final String testUrl, final int responseCode, final String body )
        throws Exception
    {
        server.expect( testUrl, responseCode, body );
    }

    public void expect( final String testUrl, final int responseCode )
        throws Exception
    {
        server.expect( testUrl, responseCode, null );
    }

}
