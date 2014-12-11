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
package org.commonjava.aprox.folo.bind.vertx;

import static org.apache.commons.codec.digest.DigestUtils.shaHex;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatCreatedResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.core.ctl.ContentController.LISTING_HTML_FILE;
import static org.commonjava.aprox.folo.FoloAddOn.TRACKING_ID_PATH_PARAM;
import static org.commonjava.vertx.vabr.types.BuiltInParam._routeBase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.bind.vertx.util.VertxRequestUtils;
import org.commonjava.aprox.core.bind.vertx.ContentAccessHandler;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.folo.ctl.FoloContentController;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.subsys.datafile.conf.DataFileConfiguration;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.aprox.util.RequestUtils;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.BindingType;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.RouteHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.file.AsyncFile;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.streams.Pump;

/**
 * Modified copy of {@link ContentAccessHandler} that collects a tracking ID in addition to store type and name, then hands this off to
 * {@link FoloContentController} (a variant of {@link ContentController}), which records artifact accesses.
 * 
 * NOTE: This is a result of copy/paste programming, so changes to {@link ContentAccessHandler} will have to be ported over.
 * 
 * @author jdcasey
 */
@Handles( "/folo/track/:type=(hosted|group|remote)/:name/:id/:path=(.+)" )
public class FoloContentAccessHandler
    implements RequestHandler
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FoloContentController contentController;

    @Inject
    private DataFileConfiguration config;

    @Inject
    private Vertx vertx;

    @Inject
    private UriFormatter uriFormatter;

    protected FoloContentAccessHandler()
    {
    }

    public FoloContentAccessHandler( final FoloContentController controller, final DataFileConfiguration config,
                                     final Vertx vertx, final UriFormatter uriFormatter )
    {
        this.contentController = controller;
        this.config = config;
        this.vertx = vertx;
        this.uriFormatter = uriFormatter;
    }

    @Routes( { @Route( method = Method.PUT, binding = BindingType.raw, fork = false ) } )
    public void doCreate( final HttpServerRequest request )
    {
        request.pause();

        final String name = request.params()
                                   .get( PathParam.name.key() );
        final String path = request.params()
                                   .get( PathParam.path.key() );

        final String type = request.params()
                                   .get( PathParam.type.key() );

        final String id = request.params()
                                 .get( TRACKING_ID_PATH_PARAM );

        final StoreType st = StoreType.get( type );

        final TrackingKey tk = new TrackingKey( id, new StoreKey( st, name ) );

        final File dir = new File( config.getWorkBasedir(), "pending-uploads" );
        dir.getAbsoluteFile()
           .mkdirs();

        final String fname = shaHex( type + ":" + name + "/" + path + "." + System.currentTimeMillis() );

        final File workFile = new File( dir, fname );

        // FIXME: What a horrible mess!
        vertx.fileSystem()
             .open( workFile.getAbsolutePath(), new AsyncResultHandler<AsyncFile>()
             {
                 @Override
                 public void handle( final AsyncResult<AsyncFile> ar )
                 {
                     if ( ar.failed() )
                     {
                         final Throwable error = ar.cause();
                         logger.error( String.format( "Failed to upload: %s to: %s. Reason: %s", path, name,
                                                      error.getMessage() ), error );

                         formatResponse( error, request );
                         return;
                     }

                     final AsyncFile file = ar.result();
                     final Pump pump = Pump.createPump( request, file );

                     request.endHandler( new Handler<Void>()
                     {
                         @Override
                         public void handle( final Void event )
                         {
                             file.close( new AsyncResultHandler<Void>()
                             {
                                 @Override
                                 public void handle( final AsyncResult<Void> ar )
                                 {
                                     if ( ar.failed() )
                                     {
                                         final Throwable error = ar.cause();
                                         logger.error( String.format( "Failed to upload: %s to: %s. Reason: %s", path,
                                                                      name, error.getMessage() ), error );

                                         formatResponse( error, request );
                                         return;
                                     }

                                     logger.info( "Creating Path: {}\nin: {}:{}", path, st, name );

                                     InputStream stream = null;
                                     try
                                     {
                                         stream = new FileInputStream( workFile );
                                         final Transfer stored = contentController.store( tk, path, stream );
                                         logger.info( "Stored: {}", stored );

                                         final StoreKey storageKey = LocationUtils.getKey( stored );
                                         logger.info( "Key for storage location: {}", storageKey );

                                         formatCreatedResponse( request, uriFormatter, st.singularEndpointName(),
                                                                storageKey.getName(), stored.getPath() ).end();
                                     }
                                     catch ( final AproxWorkflowException | IOException e )
                                     {
                                         logger.error( String.format( "Failed to upload: %s to: %s. Reason: %s", path,
                                                                      name, e.getMessage() ), e );
                                         formatResponse( e, request );
                                     }
                                     finally
                                     {
                                         closeQuietly( stream );
                                         workFile.delete();
                                     }
                                 }
                             } );
                         }
                     } );

                     pump.start();
                     request.resume();
                 }
             } );
    }

    @Routes( { @Route( method = Method.HEAD, fork = false ) } )
    public void doHead( final HttpServerRequest request )
    {
        //        request.pause();
        //        request.bodyHandler( new Handler<Buffer>()
        //        {
        //            @Override
        //            public void handle( final Buffer event )
        //            {
        final String name = request.params()
                                   .get( PathParam.name.key() );
        final String path = request.params()
                                   .get( PathParam.path.key() );

        final String type = request.params()
                                   .get( PathParam.type.key() );

        final String id = request.params()
                                 .get( TRACKING_ID_PATH_PARAM );

        final StoreType st = StoreType.get( type );

        final TrackingKey tk = new TrackingKey( id, new StoreKey( st, name ) );

        final String standardAccept = VertxRequestUtils.getStandardAccept( request, ApplicationContent.text_html );

        String givenAccept = request.headers()
                                    .get( RouteHeader.accept.header() );
        if ( givenAccept == null )
        {
            givenAccept = standardAccept;
        }

        try
        {
            final String baseUri = request.params()
                                          .get( _routeBase.key() );

            if ( path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
            {
                logger.info( "Getting listing at: {}", path );
                final String content =
                    contentController.renderListing( standardAccept, tk, path, baseUri, uriFormatter );

                request.response()
                       .putHeader( ApplicationHeader.content_type.key(), givenAccept )
                       .putHeader( ApplicationHeader.content_length.key(), Long.toString( content.length() ) )
                       .putHeader( ApplicationHeader.last_modified.key(), RequestUtils.formatDateHeader( new Date() ) )
                       .end();
                request.response()
                       .close();
            }
            else
            {
                final Transfer item = contentController.get( tk, path );

                final String contentType = contentController.getContentType( path );

                request.response()
                       .putHeader( ApplicationHeader.content_type.key(), contentType )
                       .putHeader( ApplicationHeader.content_length.key(), Long.toString( item.getDetachedFile()
                                                                                              .length() ) )
                       .putHeader( ApplicationHeader.last_modified.key(),
                                   RequestUtils.formatDateHeader( item.getDetachedFile()
                                                                      .lastModified() ) )
                       .end();
                request.response()
                       .close();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            formatResponse( e, request );
        }
    }

    @Routes( { @Route( method = Method.GET ) } )
    public void doGet( final HttpServerRequest request )
    {
        final String name = request.params()
                                   .get( PathParam.name.key() );
        final String path = request.params()
                                   .get( PathParam.path.key() );

        final String type = request.params()
                                   .get( PathParam.type.key() );

        final String id = request.params()
                                 .get( TRACKING_ID_PATH_PARAM );

        final StoreType st = StoreType.get( type );

        final TrackingKey tk = new TrackingKey( id, new StoreKey( st, name ) );

        final String standardAccept = VertxRequestUtils.getStandardAccept( request, ApplicationContent.text_html );
        String givenAccept = request.headers()
                                    .get( RouteHeader.recommended_content_type.header() );
        if ( givenAccept == null )
        {
            givenAccept = standardAccept;
        }

        logger.info( "User asked for: {}\nStandard accept header for that is: {}", givenAccept, standardAccept );

        try
        {
            final String baseUri = request.params()
                                          .get( _routeBase.key() );

            if ( path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
            {
                //                logger.info( "Redirecting to index.html under: {}", path );
                //                formatRedirect( request, uriFormatter.formatAbsolutePathTo( baseUri, getStoreType().singularEndpointName(), name, path, LISTING_FILE ) );
                //            }
                //            else if ( path.endsWith( LISTING_FILE ) )
                //            {
                logger.info( "Getting listing at: {}", path );
                final String content =
                    contentController.renderListing( standardAccept, tk, path, baseUri, uriFormatter );

                formatOkResponseWithEntity( request, content, givenAccept );
            }
            else
            {
                final Transfer item = contentController.get( tk, path );
                if ( item.isDirectory()
                    || ( path.lastIndexOf( '.' ) < path.lastIndexOf( '/' ) && contentController.isHtmlContent( item ) ) )
                {
                    item.delete( false );

                    logger.info( "Getting listing at: {}", path + "/" );
                    final String content =
                        contentController.renderListing( standardAccept, tk, path + "/", baseUri, uriFormatter );

                    formatOkResponseWithEntity( request, content, givenAccept );
                }
                else
                {
                    final String contentType = contentController.getContentType( path );

                    item.touch();

                    request.resume()
                           .response()
                           .putHeader( ApplicationHeader.content_type.key(), contentType )
                           .sendFile( item.getDetachedFile()
                                          .getCanonicalPath() )
                           .close();
                }
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            formatResponse( e, request );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            formatResponse( e, request );
        }
    }

}
