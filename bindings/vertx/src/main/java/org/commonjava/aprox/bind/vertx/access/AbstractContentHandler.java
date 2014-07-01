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
package org.commonjava.aprox.bind.vertx.access;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatCreatedResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatRedirect;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.core.rest.ContentController.LISTING_HTML_FILE;
import static org.commonjava.vertx.vabr.types.BuiltInParam._classContextUrl;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.core.rest.ContentController;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.aprox.util.RequestUtils;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.vertx.vabr.util.VertXInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

public abstract class AbstractContentHandler<T extends ArtifactStore>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentController contentController;

    @Inject
    private UriFormatter uriFormatter;

    protected AbstractContentHandler()
    {
    }

    protected AbstractContentHandler( final ContentController controller, final UriFormatter uriFormatter )
    {
        this.contentController = controller;
        this.uriFormatter = uriFormatter;
    }

    protected void doCreate( final HttpServerRequest request )
    {
        request.pause();
        final String name = request.params()
                                   .get( PathParam.name.key() );
        final String path = request.params()
                                   .get( PathParam.path.key() );

        final String contentLen = request.headers()
                                         .get( ApplicationHeader.content_length.key() );
        final VertXInputStream stream =
            contentLen == null ? new VertXInputStream( request ) : new VertXInputStream( request,
                                                                                         Long.parseLong( contentLen ) );
        try
        {
            final Transfer stored = getContentController().store( getStoreType(), name, path, stream );
            final StoreKey storageKey = LocationUtils.getKey( stored );

            formatCreatedResponse( request, uriFormatter, getStoreType().singularEndpointName(), storageKey.getName(),
                                   stored.getPath() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to upload: %s to: %s. Reason: %s", path, name, e.getMessage() ), e );
            formatResponse( e, request );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            try
            {
                request.response()
                       .end();
            }
            catch ( final IllegalStateException e )
            {
            }
        }
    }

    protected void doDelete( final HttpServerRequest request )
    {
        final String name = request.params()
                                   .get( PathParam.name.key() );
        final String path = request.params()
                                   .get( PathParam.path.key() );

        try
        {
            final ApplicationStatus result = contentController.delete( getStoreType(), name, path );
            request.response()
                   .setStatusCode( result.code() )
                   .setStatusMessage( result.message() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to delete artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            formatResponse( e, request );
        }
    }

    protected abstract StoreType getStoreType();

    protected void doHead( final HttpServerRequest request )
    {
        // TODO:
        // directory request (ends with "/") or empty path (directory request for proxy root)
        // browse via redirect to browser resource...giving client the option to intercept redirection.
        // Likewise, browse resource should redirect here when accessing concrete files.

        final String name = request.params()
                                   .get( PathParam.name.key() );
        final String path = request.params()
                                   .get( PathParam.path.key() );

        try
        {
            final String baseUri = request.params()
                                          .get( _classContextUrl.key() );

            if ( path.endsWith( "/" ) )
            {
                logger.info( "Redirecting to index.html under: {}", path );
                formatRedirect( request, uriFormatter.formatAbsolutePathTo( baseUri,
                                                                            getStoreType().singularEndpointName(),
                                                                            name, path, LISTING_HTML_FILE ) );
            }
            else if ( path.endsWith( LISTING_HTML_FILE ) )
            {
                logger.info( "Getting listing at: {}", path );
                final String html = contentController.list( getStoreType(), name, path, baseUri, uriFormatter );

                request.response()
                       .putHeader( ApplicationHeader.content_type.key(), ApplicationContent.text_html )
                       .putHeader( ApplicationHeader.content_length.key(), Long.toString( html.length() ) )
                       .putHeader( ApplicationHeader.last_modified.key(), RequestUtils.formatDateHeader( new Date() ) )
                       .end();
                request.response()
                       .close();
            }
            else
            {
                final Transfer item = contentController.get( getStoreType(), name, path );

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

    protected void doGet( final HttpServerRequest request )
    {
        // TODO:
        // directory request (ends with "/") or empty path (directory request for proxy root)
        // browse via redirect to browser resource...giving client the option to intercept redirection.
        // Likewise, browse resource should redirect here when accessing concrete files.

        final String name = request.params()
                                   .get( PathParam.name.key() );
        final String path = request.params()
                                   .get( PathParam.path.key() );

        try
        {
            final String baseUri = request.params()
                                          .get( _classContextUrl.key() );

            if ( path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
            {
                //                logger.info( "Redirecting to index.html under: {}", path );
                //                formatRedirect( request, uriFormatter.formatAbsolutePathTo( baseUri, getStoreType().singularEndpointName(), name, path, LISTING_FILE ) );
                //            }
                //            else if ( path.endsWith( LISTING_FILE ) )
                //            {
                logger.info( "Getting listing at: {}", path );
                final String html = contentController.list( getStoreType(), name, path, baseUri, uriFormatter );

                formatOkResponseWithEntity( request, html, ApplicationContent.text_html );
            }
            else
            {
                final Transfer item = contentController.get( getStoreType(), name, path );
                if ( item.isDirectory()
                    || ( path.lastIndexOf( '.' ) < path.lastIndexOf( '/' ) && contentController.isHtmlContent( item ) ) )
                {
                    item.delete( false );

                    logger.info( "Getting listing at: {}", path + "/" );
                    final String html =
                        contentController.list( getStoreType(), name, path + "/", baseUri, uriFormatter );

                    formatOkResponseWithEntity( request, html, ApplicationContent.text_html );
                }
                else
                {
                    final String contentType = contentController.getContentType( path );

                    request.response()
                           .putHeader( ApplicationHeader.content_type.key(), contentType );

                    item.touch();
                    request.response()
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

    protected ContentController getContentController()
    {
        return contentController;
    }

}
