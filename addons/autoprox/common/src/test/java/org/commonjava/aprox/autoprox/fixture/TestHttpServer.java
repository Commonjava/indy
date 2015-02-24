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

import static org.commonjava.maven.galley.util.PathUtils.normalize;
import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHttpServer
    extends ExternalResource
{

    private static final int TRIES = 4;

    private static Random rand = new Random();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final int port;

    private final ExpectationServlet servlet;

    private Undertow server;

    public TestHttpServer( final String baseResource )
    {
        servlet = new ExpectationServlet( baseResource );

        int port = -1;
        ServerSocket ss = null;
        for ( int i = 0; i < TRIES; i++ )
        {
            final int p = Math.abs( rand.nextInt() ) % 2000 + 8000;
            logger.info( "Trying port: {}", p );
            try
            {
                ss = new ServerSocket( p );
                port = p;
                break;
            }
            catch ( final IOException e )
            {
                logger.error( String.format( "Port %s failed. Reason: %s", p, e.getMessage() ), e );
            }
            finally
            {
                IOUtils.closeQuietly( ss );
            }
        }

        if ( port < 8000 )
        {
            throw new RuntimeException( "Failed to start test HTTP server. Cannot find open port in " + TRIES
                + " tries." );
        }

        this.port = port;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public void after()
    {
        if ( server != null )
        {
            server.stop();
        }
    }

    @Override
    public void before()
        throws Exception
    {
        final ServletInfo si = Servlets.servlet( "TEST", ExpectationServlet.class )
                                       .addMapping( "*" )
                                       .addMapping( "/*" )
                                       .setLoadOnStartup( 1 );

        si.setInstanceFactory( new ImmediateInstanceFactory<Servlet>( servlet ) );

        final DeploymentInfo di = new DeploymentInfo().addServlet( si )
                                                      .setDeploymentName( "TEST" )
                                                      .setContextPath( "/" )
                                                      .setClassLoader( Thread.currentThread()
                                                                             .getContextClassLoader() );

        final DeploymentManager dm = Servlets.defaultContainer()
                                             .addDeployment( di );
        dm.deploy();

        server = Undertow.builder()
                         .setHandler( dm.start() )
                         .addHttpListener( port, "127.0.0.1" )
                         .build();

        server.start();
    }

    public static final class ExpectationServlet
        extends HttpServlet
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        private static final long serialVersionUID = 1L;

        private final String baseResource;

        private final Map<String, Expectation> expectations = new HashMap<String, Expectation>();

        private final Map<String, Integer> accessesByPath = new HashMap<String, Integer>();

        private final Map<String, String> errors = new HashMap<String, String>();

        public ExpectationServlet()
        {
            logger.error( "Default constructor not actually supported!!!" );
            this.baseResource = "/";
        }

        public ExpectationServlet( final String baseResource )
        {
            this.baseResource = baseResource;
        }

        public Map<String, Integer> getAccessesByPath()
        {
            return accessesByPath;
        }

        public Map<String, String> getRegisteredErrors()
        {
            return errors;
        }

        public String getBaseResource()
        {
            return baseResource;
        }

        public void registerException( final String url, final String error )
        {
            this.errors.put( url, error );
        }

        public void expect( final String testUrl, final int responseCode, final String body )
            throws Exception
        {
            final URL url = new URL( testUrl );
            final String path = url.getPath();

            logger.info( "Registering expection: '{}', code: {}, body:\n{}", path, responseCode, body );
            expectations.put( path, new Expectation( path, responseCode, body ) );
        }

        @Override
        protected void doGet( final HttpServletRequest req, final HttpServletResponse resp )
            throws ServletException, IOException
        {
            String wholePath;
            try
            {
                wholePath = new URI( req.getRequestURI() ).getPath();
            }
            catch ( final URISyntaxException e )
            {
                throw new ServletException( "Cannot parse request URI", e );
            }

            String path = wholePath;
            if ( path.length() > 1 )
            {
                path = path.substring( 1 );
            }

            final Integer i = accessesByPath.get( wholePath );
            if ( i == null )
            {
                accessesByPath.put( wholePath, 1 );
            }
            else
            {
                accessesByPath.put( wholePath, i + 1 );
            }

            if ( errors.containsKey( wholePath ) )
            {
                final String error = errors.get( wholePath );
                logger.error( "Returning registered error: {}", error );
                resp.sendError( 500 );

                return;
            }

            logger.info( "Looking for expectation: '{}'", wholePath );
            final Expectation expectation = expectations.get( wholePath );
            if ( expectation != null )
            {
                logger.info( "Responding via registered expectation: {}", expectation );

                resp.setStatus( expectation.code() );

                if ( expectation.body() != null )
                {
                    resp.getWriter()
                        .write( expectation.body() );
                }

                return;
            }

            resp.setStatus( 404 );
        }

    }

    public String formatUrl( final String... subpath )
    {
        return String.format( "http://127.0.0.1:%s/%s/%s", port, servlet.getBaseResource(), normalize( subpath ) );
    }

    public String getBaseUri()
    {
        return String.format( "http://127.0.0.1:%s/%s", port, servlet.getBaseResource() );
    }

    public String getUrlPath( final String url )
        throws MalformedURLException
    {
        return new URL( url ).getPath();
    }

    public Map<String, Integer> getAccessesByPath()
    {
        return servlet.getAccessesByPath();
    }

    public Map<String, String> getRegisteredErrors()
    {
        return servlet.getRegisteredErrors();
    }

    public void registerException( final String url, final String error )
    {
        servlet.registerException( url, error );
    }

    public void expect( final String testUrl, final int responseCode, final String body )
        throws Exception
    {
        servlet.expect( testUrl, responseCode, body );
    }

    private static final class Expectation
    {
        private final int code;

        private final String body;

        private final String url;

        Expectation( final String url, final int code, final String body )
        {
            this.url = url;
            this.code = code;
            this.body = body;
        }

        int code()
        {
            return code;
        }

        String body()
        {
            return body;
        }

        @Override
        public String toString()
        {
            return "Expect (" + url + "), and respond with code:" + code() + ", body:\n" + body();
        }
    }

}
