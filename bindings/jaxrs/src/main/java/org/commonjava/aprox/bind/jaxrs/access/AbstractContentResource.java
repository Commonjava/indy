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
package org.commonjava.aprox.bind.jaxrs.access;

import static org.commonjava.aprox.core.rest.ContentController.LISTING_HTML_FILE;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.core.rest.ContentController;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractContentResource<T extends ArtifactStore>
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentController contentController;

    protected AbstractContentResource()
    {
    }

    protected Response doCreate( final String name, final String path, final HttpServletRequest request,
                                 final UriInfo uriInfo )
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
            logger.error( String.format( "Failed to open stream from request: %s", e.getMessage() ), e );
            response = Response.serverError()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to upload: %s to: %s. Reason: %s", path, name, e.getMessage() ), e );
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
            logger.error( String.format( "Failed to delete artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
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

        try
        {
            final UriFormatter uriFormatter = new JaxRsUriFormatter( uriInfo );

            if ( path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
            {
                //                final String ref = uriFormatter.formatAbsolutePathTo( getStoreType().singularEndpointName(), name, path, LISTING_HTML_FILE );
                //
                //                logger.info( "Redirecting to index.html under: {}\n  ({})", path, ref );
                //                response = Response.seeOther( new URI( ref ) )
                //                                   .build();
                //            }
                //            else if ( path.endsWith( LISTING_HTML_FILE ) )
                //            {
                final String svcPath = uriInfo.getBaseUri()
                                              .toString();

                logger.info( "Getting listing at: {} (service path: {})", path, svcPath );
                final String html = contentController.list( getStoreType(), name, path, "/", uriFormatter );

                response = Response.ok( html, MediaType.TEXT_HTML )
                                   .build();
            }
            else
            {
                final Transfer item = contentController.get( getStoreType(), name, path );
                if ( item.isDirectory()
                    || ( path.lastIndexOf( '.' ) < path.lastIndexOf( '/' ) && contentController.isHtmlContent( item ) ) )
                {
                    item.delete( false );

                    final String svcPath = uriInfo.getBaseUri()
                                                  .toString();

                    logger.info( "Getting listing at: {} (service path: {})", path + "/", svcPath );
                    final String html = contentController.list( getStoreType(), name, path + "/", "/", uriFormatter );

                    response = Response.ok( html, MediaType.TEXT_HTML )
                                       .build();
                }
                else
                {

                    final String contentType = contentController.getContentType( path );

                    response = Response.ok( item.openInputStream(), contentType )
                                       .build();
                }
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = AproxExceptionUtils.formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = Response.serverError()
                               .build();
        }
        //        catch ( final URISyntaxException e )
        //        {
        //            logger.error( String.format( "Failed to format relocation to index.html from: %s from: %s. Reason: %s",
        //                                         path, name, e.getMessage() ), e );
        //            response = Response.serverError()
        //                               .build();
        //        }

        return response;
    }

    protected ContentController getContentController()
    {
        return contentController;
    }

}
