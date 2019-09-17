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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.commonjava.propulsor.boot.PortFinder;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.atlas.maven.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.parse.PomPeek;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractHttproxFunctionalTest
    extends AbstractIndyFunctionalTest
{

    protected static final String HOST = "127.0.0.1";

    protected static final String DEFAULT_BASE_HTTPROX_CONFIG = "[httprox]\nenabled=true\nport=${proxyPort}" ;

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public ExpectationServer server = new ExpectationServer();

    protected int proxyPort;

    protected HttpClientContext proxyContext( final String user, final String pass )
    {
        final CredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials( new AuthScope( HOST, proxyPort ), new UsernamePasswordCredentials( user, pass ) );
        final HttpClientContext ctx = HttpClientContext.create();
        ctx.setCredentialsProvider( creds );

        return ctx;
    }

    protected CloseableHttpClient proxiedHttp() throws Exception
    {
        return proxiedHttp( null, null, null );
    }

    protected CloseableHttpClient proxiedHttp( SSLSocketFactory socketFactory ) throws Exception
    {
        return proxiedHttp( null, null, socketFactory );
    }

    protected CloseableHttpClient proxiedHttp( final String user, final String pass ) throws Exception
    {
        return proxiedHttp( user, pass, null );
    }

    protected CloseableHttpClient proxiedHttp( final String user, final String pass, SSLSocketFactory socketFactory ) throws Exception
    {
        CredentialsProvider creds = null;

        if ( user != null )
        {
            creds = new BasicCredentialsProvider();
            creds.setCredentials( new AuthScope( HOST, proxyPort ), new UsernamePasswordCredentials( user, pass ) );
        }

        HttpHost proxy = new HttpHost( HOST, proxyPort );

        final HttpRoutePlanner planner = new DefaultProxyRoutePlanner( proxy );
        HttpClientBuilder builder = HttpClients.custom()
                                         .setRoutePlanner( planner )
                                         .setDefaultCredentialsProvider( creds )
                                         .setProxy( proxy )
                                         .setSSLSocketFactory( socketFactory );

        return builder.build();
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
        throws IOException
    {
        proxyPort = PortFinder.findOpenPort( 16 );

        String baseHttproxConfig = getBaseHttproxConfig();
        if ( isEmpty( baseHttproxConfig ) )
        {
            baseHttproxConfig = DEFAULT_BASE_HTTPROX_CONFIG;
        }

        baseHttproxConfig = baseHttproxConfig.replaceAll( "\\$\\{proxyPort\\}", Integer.toString( proxyPort ) );

        String additionalConfig = getAdditionalHttproxConfig();
        if ( additionalConfig == null )
        {
            additionalConfig = "";
        }

        writeConfigFile( "conf.d/httprox.conf", baseHttproxConfig + "\n" + additionalConfig );
    }

    protected String getBaseHttproxConfig()
    {
        return "";
    }

    protected String getAdditionalHttproxConfig()
    {
        return "";
    }

    protected PomRef loadPom( final String name, final Map<String, String> substitutions )
    {
        try
        {
            final InputStream stream = Thread.currentThread()
                                             .getContextClassLoader()
                                             .getResourceAsStream( name.endsWith( ".pom" ) ? name : name + ".pom" );

            String pom = IOUtils.toString( stream );
            IOUtils.closeQuietly( stream );

            for ( final Map.Entry<String, String> entry : substitutions.entrySet() )
            {
                pom = pom.replace( "@" + entry.getKey() + "@", entry.getValue() );
            }

            final PomPeek peek = new PomPeek( pom, false );
            final ProjectVersionRef gav = peek.getKey();

            final String path =
                String.format( "%s/%s/%s/%s-%s.pom", gav.getGroupId()
                                                        .replace( '.', '/' ), gav.getArtifactId(),
                               gav.getVersionString(), gav.getArtifactId(), gav.getVersionString() );

            return new PomRef( pom, path );
        }
        catch ( final Exception e )
        {
            e.printStackTrace();
            fail( "Failed to read POM from: " + name );
        }

        return null;
    }

    protected static final class PomRef
    {
        PomRef( final String pom, final String path )
        {
            this.pom = pom;
            this.path = path;
        }

        protected final String pom;

        protected final String path;
    }
}
