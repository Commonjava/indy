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
package org.commonjava.aprox.bind.jaxrs.access;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.core.rest.ContentController;
import org.commonjava.aprox.core.util.UriFormatter;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSimpleAccessResource<T extends ArtifactStore>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentController contentController;

    protected AbstractSimpleAccessResource()
    {
    }

    protected Response doCreate( final String name, final String path, final HttpServletRequest request, final UriInfo uriInfo )
    {
        Response response = null;
        try
        {
            final Transfer stored = getContentController().store( getStoreType(), name, path, request.getInputStream() );

            final StoreKey storageKey = LocationUtils.getKey( stored );
            response = Response.created( uriInfo.getAbsolutePathBuilder()
                                                .path( storageKey.getName() )
                                                .path( path )
                                                .build() )
                               .build();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to open stream from request: {}", e, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to upload: {} to: {}. Reason: {}", e, path, name, e.getMessage() );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

    protected Response doDelete( final String name, final String path )
    {
        Response response = null;

        try
        {
            final ApplicationStatus result = contentController.delete( getStoreType(), name, path );
            response = Response.status( result.code() )
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to delete artifact: {} from: {}. Reason: {}", e, path, name, e.getMessage() );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

    protected abstract StoreType getStoreType();

    protected Response doGet( final String name, final String path, final UriInfo uriInfo )
    {
        // TODO:
        // directory request (ends with "/") or empty path (directory request for proxy root)
        // browse via redirect to browser resource...giving client the option to intercept redirection.
        // Likewise, browse resource should redirect here when accessing concrete files.

        Response response = null;

        final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();
        try
        {
            final UriFormatter uriFormatter = new JaxRsUriFormatter( uriBuilder );

            if ( path.endsWith( "/" ) )
            {
                logger.info( "Redirecting to index.html under: {}", path );
                response =
                    Response.seeOther( new URI( uriFormatter.formatAbsolutePathTo( uriBuilder.path( getClass() )
                                                                                             .build()
                                                                                             .toString(), getStoreType().singularEndpointName(),
                                                                                   name, path, "index.html" ) ) )
                            .build();
            }
            else if ( path.endsWith( "index.html" ) )
            {
                logger.info( "Getting listing at: {}", path );
                final String html = contentController.list( getStoreType(), name, path, uriBuilder.path( getClass() )
                                                                                                  .build()
                                                                                                  .toString(), uriFormatter );

                Response.ok( html, MediaType.TEXT_HTML );
            }
            else
            {
                final Transfer item = contentController.get( getStoreType(), name, path );
                final String contentType = contentController.getContentType( path );

                response = Response.ok( item.openInputStream(), contentType )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to download artifact: {} from: {}. Reason: {}", e, path, name, e.getMessage() );
            response = AproxExceptionUtils.formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to download artifact: {} from: {}. Reason: {}", e, path, name, e.getMessage() );
            response = Response.serverError()
                               .build();
        }
        catch ( final URISyntaxException e )
        {
            logger.error( "Failed to format relocation to index.html from: {} from: {}. Reason: {}", e, path, name, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        return response;
    }

    protected ContentController getContentController()
    {
        return contentController;
    }

}
