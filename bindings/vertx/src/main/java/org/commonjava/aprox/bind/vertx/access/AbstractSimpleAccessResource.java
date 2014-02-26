/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.bind.vertx.access;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatCreatedResponse;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatRedirect;
import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;
import static org.commonjava.vertx.vabr.types.BuiltInParam._classContextUrl;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.core.rest.ContentController;
import org.commonjava.aprox.core.util.UriFormatter;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationContent;
import org.commonjava.aprox.rest.util.ApplicationHeader;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.vertx.vabr.util.VertXInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

public abstract class AbstractSimpleAccessResource<T extends ArtifactStore>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentController contentController;

    @Inject
    private UriFormatter uriFormatter;

    protected AbstractSimpleAccessResource()
    {
    }

    protected AbstractSimpleAccessResource( final ContentController controller, final UriFormatter uriFormatter )
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
            contentLen == null ? new VertXInputStream( request ) : new VertXInputStream( request, Long.parseLong( contentLen ) );
        try
        {
            final Transfer stored = getContentController().store( getStoreType(), name, path, stream );
            final StoreKey storageKey = LocationUtils.getKey( stored );

            formatCreatedResponse( request, uriFormatter, getStoreType().singularEndpointName(), storageKey.getName(), stored.getPath() );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to upload: {} to: {}. Reason: {}", e, path, name, e.getMessage() );
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
            logger.error( "Failed to delete artifact: {} from: {}. Reason: {}", e, path, name, e.getMessage() );
            formatResponse( e, request );
        }
    }

    protected abstract StoreType getStoreType();

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

            if ( path.endsWith( "/" ) )
            {
                logger.info( "Redirecting to index.html under: {}", path );
                formatRedirect( request, uriFormatter.formatAbsolutePathTo( baseUri, getStoreType().singularEndpointName(), name, path, "index.html" ) );
            }
            else if ( path.endsWith( "index.html" ) )
            {
                logger.info( "Getting listing at: {}", path );
                final String html = contentController.list( getStoreType(), name, path, baseUri, uriFormatter );

                formatOkResponseWithEntity( request, html, ApplicationContent.text_html );
            }
            else
            {
                final Transfer item = contentController.get( getStoreType(), name, path );

                final String contentType = contentController.getContentType( path );

                request.response()
                       .putHeader( ApplicationHeader.content_type.key(), contentType );

                request.response()
                       .sendFile( item.getDetachedFile()
                                      .getCanonicalPath() );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to download artifact: {} from: {}. Reason: {}", e, path, name, e.getMessage() );
            formatResponse( e, request );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to download artifact: {} from: {}. Reason: {}", e, path, name, e.getMessage() );
            formatResponse( e, request );
        }
    }

    protected ContentController getContentController()
    {
        return contentController;
    }

}
