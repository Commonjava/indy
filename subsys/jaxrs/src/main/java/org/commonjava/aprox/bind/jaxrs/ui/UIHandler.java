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
package org.commonjava.aprox.bind.jaxrs.ui;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.model.util.HttpUtils.formatDateHeader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.conf.UIConfiguration;
import org.commonjava.aprox.util.ApplicationHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/{path: (.+)}" )
@UIApp
public class UIHandler
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private UIConfiguration config;

    private final FileTypeMap typeMap = MimetypesFileTypeMap.getDefaultFileTypeMap();

    @GET
    @HEAD
    public Response handleUIRequest( @javax.ws.rs.PathParam( "path" ) String path, @Context final Request request )
    {
        final String method = request.getMethod()
                                     .toUpperCase();
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
                    path = path.substring( 3 );
                    final URL resource = Thread.currentThread()
                                               .getContextClassLoader()
                                               .getResource( path );

                    return sendURL( resource, method );
                }

                final File uiDir = config.getUIDir();
                logger.debug( "UI basedir: '{}'", uiDir );

                final File resource = new File( uiDir, path );
                return sendFile( resource, method );
            }
            default:
            {
                logger.error( "cannot handle request for method: {}", method );
                return Response.status( Status.BAD_REQUEST )
                               .entity( "Cannot handle method: " + method )
                               .build();
            }
        }
    }

    private Response sendURL( final URL resource, final String method )
    {
        logger.debug( "Checking for existence of: '{}'", resource );
        if ( resource != null )
        {
            byte[] data;
            try
            {
                data = IOUtils.toByteArray( resource );
            }
            catch ( final IOException e )
            {
                return formatResponse( e );
            }

            if ( method == "GET" )
            {
                logger.debug( "sending file" );
                return Response.ok( new ByteArrayInputStream( data ) )
                               .header( ApplicationHeader.content_type.key(),
                                        typeMap.getContentType( resource.toExternalForm() ) )
                               .header( ApplicationHeader.content_length.key(), Long.toString( data.length ) )
                               .build();
            }
            else
            {
                logger.debug( "sending OK" );
                return Response.ok()
                               .header( ApplicationHeader.content_type.key(),
                                   typeMap.getContentType( resource.toExternalForm() ) )
                               .header( ApplicationHeader.content_length.key(), Long.toString( data.length ) )
                               .build();
            }
        }
        else
        {
            logger.debug( "sending 404" );
            return Response.status( Status.NOT_FOUND )
                           .build();
        }
    }

    private Response sendFile( final File resource, final String method )
    {
        logger.debug( "Checking for existence of: '{}'", resource );
        if ( resource.exists() )
        {
            if ( method == "GET" )
            {
                logger.debug( "sending file" );
                return Response.ok( resource )
                               .header( ApplicationHeader.last_modified.key(),
                                        formatDateHeader( resource.lastModified() ) )
                               .build();
            }
            else
            {
                logger.debug( "sending OK" );
                // TODO: set headers for content info...
                return Response.ok()
                               .header( ApplicationHeader.last_modified.key(),
                                        formatDateHeader( resource.lastModified() ) )
                               .header( ApplicationHeader.content_type.key(), typeMap.getContentType( resource ) )
                               .header( ApplicationHeader.content_length.key(), Long.toString( resource.length() ) )
                               .build();
            }
        }
        else
        {
            logger.debug( "sending 404" );
            return Response.status( Status.NOT_FOUND )
                           .build();
        }
    }
}
