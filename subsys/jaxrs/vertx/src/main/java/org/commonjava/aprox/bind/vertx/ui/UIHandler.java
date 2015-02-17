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
package org.commonjava.aprox.bind.vertx.ui;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.status;
import static org.commonjava.aprox.model.util.HttpUtils.formatDateHeader;
import static org.commonjava.aprox.util.ApplicationStatus.OK;
import static org.commonjava.vertx.vabr.types.Method.ANY;
import static org.commonjava.vertx.vabr.types.Method.GET;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.bind.vertx.conf.UIConfiguration;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( key = "UIHandler" )
@UIApp
public class UIHandler
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private UIConfiguration config;

    private final FileTypeMap typeMap = MimetypesFileTypeMap.getDefaultFileTypeMap();

    @Route( path = ":?path=(.+)", method = ANY )
    public void handleUIRequest( final HttpServerRequest request )
    {
        final Method method = Method.valueOf( request.method() );

        boolean ended = false;
        switch ( method )
        {
            case GET:
            case HEAD:
            {
                String path = request.params()
                                     .get( PathParam.path.key() );

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
                    ended = sendURL( resource, request, method );
                }

                final File uiDir = config.getUIDir();
                logger.debug( "UI basedir: '{}'", uiDir );

                final File resource = new File( uiDir, path );
                ended = sendFile( resource, request, method );
                break;
            }
            default:
            {
                logger.error( "cannot handle request for method: {}", method );
                Respond.to( request )
                       .badRequest( "Cannot handle: " + method )
                       .send();
            }
        }

        if ( !ended )
        {
            request.response()
                   .end();
        }
    }

    private boolean sendURL( final URL resource, final HttpServerRequest request, final Method method )
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
                formatResponse( e, request );
                return true;
            }

            if ( method == GET )
            {
                logger.debug( "sending file" );
                request.resume()
                       .response()
                       .putHeader( ApplicationHeader.content_type.key(),
                                   typeMap.getContentType( resource.toExternalForm() ) )
                       .putHeader( ApplicationHeader.content_length.key(), Long.toString( data.length ) )
                       .write( new Buffer( data ) );
            }
            else
            {
                logger.debug( "sending OK" );
                status( OK, request );
                request.resume()
                       .response()
                       .setChunked( true )
                       .putHeader( ApplicationHeader.content_type.key(),
                                   typeMap.getContentType( resource.toExternalForm() ) )
                       .putHeader( ApplicationHeader.content_length.key(), Long.toString( data.length ) );
            }
        }
        else
        {
            logger.debug( "sending 404" );
            Respond.to( request )
                   .notFound()
                   .send();
        }

        return false;
    }

    private boolean sendFile( final File resource, final HttpServerRequest request, final Method method )
    {
        logger.debug( "Checking for existence of: '{}'", resource );
        if ( resource.exists() )
        {
            if ( method == GET )
            {
                logger.debug( "sending file" );
                request.resume()
                       .response()
                       .putHeader( ApplicationHeader.last_modified.key(), formatDateHeader( resource.lastModified() ) )
                       .sendFile( resource.getAbsolutePath() );

                return true;
            }
            else
            {
                logger.debug( "sending OK" );
                // TODO: set headers for content info...
                status( OK, request );
                request.resume()
                       .response()
                       .setChunked( true )
                       .putHeader( ApplicationHeader.content_type.key(), typeMap.getContentType( resource ) )
                       .putHeader( ApplicationHeader.content_length.key(), Long.toString( resource.length() ) )
                       .putHeader( ApplicationHeader.last_modified.key(), formatDateHeader( resource.lastModified() ) );
            }
        }
        else
        {
            logger.debug( "sending 404" );
            Respond.to( request )
                   .notFound()
                   .send();
        }

        return false;
    }
}
