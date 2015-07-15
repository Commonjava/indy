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
package org.commonjava.aprox.bind.jaxrs.ui;

import static org.commonjava.aprox.model.util.HttpUtils.formatDateHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.conf.UIConfiguration;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class UIServlet
    extends HttpServlet
{

    private static final long serialVersionUID = 1L;

    public static final Collection<String> PATHS = Collections.unmodifiableCollection( Arrays.asList( "/*.html", "/",
                                                                                                      "/js/*",
                                                                                                      "/css/*",
                                                                                                      "/partials/*",
                                                                                                      "/ui-addons/*" ) );

    public static final Collection<String> METHODS = Collections.unmodifiableCollection( Arrays.asList( "GET", "HEAD",
                                                                                                        "OPTIONS" ) );

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private UIConfiguration config;

    private final FileTypeMap typeMap = MimetypesFileTypeMap.getDefaultFileTypeMap();

    @Override
    protected void service( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {
        if ( config == null )
        {
            config = CDI.current()
                        .select( UIConfiguration.class )
                        .get();
        }

        String path;
        try
        {
            path = new URI( request.getRequestURI() ).getPath();
        }
        catch ( final URISyntaxException e )
        {
            logger.error( "Cannot parse request URI", e );
            response.setStatus( 400 );
            return;
        }

        final String method = request.getMethod()
                                     .toUpperCase();

        logger.info( "{} {}", method, path );

        switch ( method )
        {
            case "GET":
            case "HEAD":
            {
                if ( path == null )
                {
                    logger.debug( "null path. Using /index.html" );
                    path = "index.html";
                }
                else if ( path.endsWith( "/" ) )
                {
                    path += "index.html";
                    logger.debug( "directory path. Using {}", path );
                }

                if ( path.startsWith( "/" ) )
                {
                    logger.debug( "Trimming leading '/' from path" );
                    path = path.substring( 1 );
                }

                if ( path.startsWith( "cp/" ) )
                {
                    logger.debug( "Handling request for classpath resource." );
                    path = path.substring( 3 );
                    final URL resource = Thread.currentThread()
                                               .getContextClassLoader()
                                               .getResource( path );

                    sendURL( response, resource, method );
                    return;
                }

                final File uiDir = config.getUIDir();
                logger.debug( "UI basedir: '{}'", uiDir );

                final File resource = new File( uiDir, path );
                logger.debug( "Trying to send file: " + resource );
                sendFile( response, resource, method );
                return;
            }
            default:
            {
                logger.error( "cannot handle request for method: {}", method );
                response.setStatus( ApplicationStatus.BAD_REQUEST.code() );
            }
        }
    }

    private void sendURL( final HttpServletResponse response, final URL resource, final String method )
    {
        logger.debug( "Checking for existence of: '{}'", resource );
        if ( resource != null )
        {
            byte[] data = null;
            try
            {
                data = IOUtils.toByteArray( resource );
            }
            catch ( final IOException e )
            {
                logger.error( String.format( "Failed to read data from resource: %s. Reason: %s", resource,
                                             e.getMessage() ), e );
                try
                {
                    response.sendError( ApplicationStatus.SERVER_ERROR.code(), "Failed to read resource: " + resource );
                }
                catch ( final IOException eResp )
                {
                    logger.warn( "Failed to send error response to client: " + eResp.getMessage(), eResp );
                }
            }

            if ( data == null )
            {
                return;
            }

            if ( method == "GET" )
            {
                logger.debug( "sending file" );
                OutputStream outputStream = null;
                try
                {
                    outputStream = response.getOutputStream();

                    outputStream.write( data );
                    outputStream.flush();
                }
                catch ( final IOException e )
                {
                    logger.error( String.format( "Failed to write to response output stream. Reason: %s",
                                                 e.getMessage() ), e );
                    try
                    {
                        response.sendError( ApplicationStatus.SERVER_ERROR.code(), "Failed to write response" );
                    }
                    catch ( final IOException eResp )
                    {
                        logger.warn( "Failed to send error response to client: " + eResp.getMessage(), eResp );
                    }
                }
            }
            else
            {
                logger.debug( "sending OK" );
                response.setStatus( ApplicationStatus.OK.code() );
                response.addHeader( ApplicationHeader.content_type.key(),
                                    typeMap.getContentType( resource.toExternalForm() ) );
                response.addHeader( ApplicationHeader.content_length.key(), Long.toString( data.length ) );
            }
        }
        else
        {
            logger.debug( "sending 404" );
            response.setStatus( ApplicationStatus.NOT_FOUND.code() );
        }
    }

    private void sendFile( final HttpServletResponse response, final File resource, final String method )
    {
        logger.debug( "Checking for existence of: '{}'", resource );
        if ( resource.exists() )
        {
            if ( method == "GET" )
            {
                logger.debug( "sending file" );
                response.addHeader( ApplicationHeader.last_modified.key(), formatDateHeader( resource.lastModified() ) );
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try
                {
                    inputStream = new FileInputStream( resource );
                    outputStream = response.getOutputStream();

                    IOUtils.copy( inputStream, outputStream );
                    outputStream.flush();
                }
                catch ( final IOException e )
                {
                    logger.error( String.format( "Failed to transfer requested resource: %s. Reason: %s", resource,
                                                 e.getMessage() ), e );
                    try
                    {
                        response.sendError( ApplicationStatus.SERVER_ERROR.code(), "Failed to write response" );
                    }
                    catch ( final IOException eResp )
                    {
                        logger.warn( "Failed to send error response to client: " + eResp.getMessage(), eResp );
                    }
                }
                finally
                {
                    IOUtils.closeQuietly( inputStream );
                }
            }
            else
            {
                logger.debug( "sending OK" );
                // TODO: set headers for content info...
                response.setStatus( ApplicationStatus.OK.code() );
                response.addHeader( ApplicationHeader.last_modified.key(), formatDateHeader( resource.lastModified() ) );

                response.addHeader( ApplicationHeader.content_type.key(), typeMap.getContentType( resource ) );
                response.addHeader( ApplicationHeader.content_length.key(), Long.toString( resource.length() ) );
            }
        }
        else
        {
            logger.debug( "sending 404" );
            response.setStatus( ApplicationStatus.NOT_FOUND.code() );
        }
    }
}
