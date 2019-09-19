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
package org.commonjava.indy.bind.jaxrs.ui;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.indy.util.MimeTyper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.commonjava.indy.model.util.HttpUtils.formatDateHeader;

public class AbstractUIServlet extends HttpServlet
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final Collection<String> METHODS = Collections.unmodifiableCollection( Arrays.asList( "GET", "HEAD",
                                                                                                        "OPTIONS" ) );

    @Inject
    private MimeTyper mimeTyper;

    protected void sendURL( final HttpServletResponse response, final URL resource, final String method )
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

            if ( "GET".equals( method ) )
            {
                logger.debug( "sending OK" );
                response.setStatus( ApplicationStatus.OK.code() );
                response.addHeader( ApplicationHeader.content_type.key(),
                                    mimeTyper.getContentType( resource.toExternalForm() ) );
                response.addHeader( ApplicationHeader.content_length.key(), Long.toString( data.length ) );

                logger.debug( "sending file" );
                OutputStream outputStream;
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
                                    mimeTyper.getContentType( resource.toExternalForm() ) );
                response.addHeader( ApplicationHeader.content_length.key(), Long.toString( data.length ) );
            }
        }
        else
        {
            logger.debug( "sending 404" );
            response.setStatus( ApplicationStatus.NOT_FOUND.code() );
        }
    }

    protected void sendFile( final HttpServletResponse response, final File resource, final String method )
    {
        logger.debug( "Checking for existence of: '{}'", resource );
        if ( resource.exists() )
        {
            if ( "GET".equals( method ) )
            {
                logger.debug( "sending file" );
                response.setStatus( ApplicationStatus.OK.code() );
                response.addHeader( ApplicationHeader.last_modified.key(), formatDateHeader( resource.lastModified() ) );

                response.addHeader( ApplicationHeader.content_type.key(), getContentType( resource.getName() ) );
                response.addHeader( ApplicationHeader.content_length.key(), Long.toString( resource.length() ) );

                InputStream inputStream = null;
                OutputStream outputStream;
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

                response.addHeader( ApplicationHeader.content_type.key(), mimeTyper.getContentType( resource.getPath() ) );
                response.addHeader( ApplicationHeader.content_length.key(), Long.toString( resource.length() ) );
            }
        }
        else
        {
            logger.debug( "sending 404" );
            response.setStatus( ApplicationStatus.NOT_FOUND.code() );
        }
    }

    private String getContentType( String resource )
    {
        if ( resource.endsWith( ".html" ) )
        {
            return ApplicationContent.text_html;
        }
        else if ( resource.endsWith( ".css" ) )
        {
            return ApplicationContent.text_css;
        }
        else if ( resource.endsWith( ".js" ) )
        {
            return ApplicationContent.application_javascript;
        }

        return mimeTyper.getContentType( resource );
    }
}
