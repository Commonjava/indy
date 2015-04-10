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
package org.commonjava.aprox.folo.bind.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.core.ctl.ContentController.LISTING_HTML_FILE;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

import javax.inject.Inject;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxDeployment;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsRequestHelper;
import org.commonjava.aprox.core.bind.jaxrs.ContentAccessHandler;
import org.commonjava.aprox.core.bind.jaxrs.util.TransferStreamingOutput;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.folo.ctl.FoloContentController;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.util.HttpUtils;
import org.commonjava.aprox.util.AcceptInfo;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modified copy of {@link ContentAccessHandler} that collects a tracking ID in addition to store type and name, then hands this off to
 * {@link FoloContentController} (a variant of {@link ContentController}), which records artifact accesses.
 * 
 * NOTE: This is a result of copy/paste programming, so changes to {@link ContentAccessHandler} will have to be ported over.
 * 
 * @author jdcasey
 */
@Path( "/api/folo/track/{id}/{type: (hosted|group|remote)}/{name}" )
public class FoloContentAccessHandler
    implements AproxResources
{

    private static final String BASE_PATH = AproxDeployment.API_PREFIX + "/folo/track";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private FoloContentController contentController;

    @Inject
    private UriFormatter uriFormatter;

    @Inject
    private JaxRsRequestHelper jaxRsRequestHelper;

    public FoloContentAccessHandler()
    {
    }

    public FoloContentAccessHandler( final FoloContentController controller, final UriFormatter uriFormatter,
                                     final JaxRsRequestHelper jaxRsRequestHelper )
    {
        this.contentController = controller;
        this.uriFormatter = uriFormatter;
        this.jaxRsRequestHelper = jaxRsRequestHelper;
    }

    @PUT
    @Path( "/{path: (.*)}" )
    public Response doCreate( @PathParam( "id" ) final String id, @PathParam( "type" ) final String type,
                              @PathParam( "name" ) final String name, @PathParam( "path" ) final String path,
                              @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final StoreType st = StoreType.get( type );

        final TrackingKey tk = new TrackingKey( id, new StoreKey( st, name ) );

        Response response = null;
        Transfer transfer;
        try
        {
            final ServletInputStream inputStream = request.getInputStream();
            transfer = contentController.store( tk, path, inputStream );

            final StoreKey storageKey = LocationUtils.getKey( transfer );
            logger.info( "Key for storage location: {}", storageKey );

            final URI uri = uriInfo.getBaseUriBuilder()
                                   .path( getClass() )
                                   .path( path )
                                   .build( id, type, name );

            response = Response.created( uri )
                               .build();
        }
        catch ( final IOException e )
        {
            response = formatResponse( e, true );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to upload: %s to: %s. Reason: %s", path, name, e.getMessage() ), e );

            response = formatResponse( e );
        }

        return response;
    }

    @HEAD
    @Path( "/{path: (.*)}" )
    public Response doHead( @PathParam( "id" ) final String id, @PathParam( "type" ) final String type,
                            @PathParam( "name" ) final String name, @PathParam( "path" ) final String path,
                            @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final StoreType st = StoreType.get( type );

        final TrackingKey tk = new TrackingKey( id, new StoreKey( st, name ) );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );

        Response response;
        try
        {
            final String baseUri = uriInfo.getBaseUriBuilder()
                                          .path( BASE_PATH )
                                          .path( id )
                                          .build()
                                          .toString();

            if ( path == null || path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
            {
                logger.info( "Getting listing at: {}", path );
                final String content =
                    contentController.renderListing( acceptInfo.getBaseAccept(), tk, path, baseUri, uriFormatter );

                response =
                    Response.ok()
                            .header( ApplicationHeader.content_type.key(), acceptInfo.getRawAccept() )
                            .header( ApplicationHeader.content_length.key(), Long.toString( content.length() ) )
                            .header( ApplicationHeader.last_modified.key(), HttpUtils.formatDateHeader( new Date() ) )
                            .build();
            }
            else
            {
                final Transfer item = contentController.get( tk, path );

                final String contentType = contentController.getContentType( path );

                response =
                    Response.ok()
                            .header( ApplicationHeader.content_type.key(), contentType )
                            .header( ApplicationHeader.content_length.key(), Long.toString( item.length() ) )
                            .header( ApplicationHeader.last_modified.key(),
                                     HttpUtils.formatDateHeader( item.lastModified() ) )
                            .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @GET
    @Path( "/{path: (.*)}" )
    public Response doGet( @PathParam( "id" ) final String id, @PathParam( "type" ) final String type,
                           @PathParam( "name" ) final String name, @PathParam( "path" ) final String path,
                           @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        final StoreType st = StoreType.get( type );

        final TrackingKey tk = new TrackingKey( id, new StoreKey( st, name ) );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );

        logger.info( "User asked for: {}\nStandard accept header for that is: {}", acceptInfo.getRawAccept(),
                     acceptInfo.getBaseAccept() );

        Response response;
        try
        {
            final String baseUri = uriInfo.getBaseUriBuilder()
                                          .path( BASE_PATH )
                                          .path( id )
                                          .build()
                                          .toString();

            if ( path == null || path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
            {
                //                logger.info( "Redirecting to index.html under: {}", path );
                //                formatRedirect( request, uriFormatter.formatAbsolutePathTo( baseUri, getStoreType().singularEndpointName(), name, path, LISTING_FILE ) );
                //            }
                //            else if ( path.endsWith( LISTING_FILE ) )
                //            {
                logger.info( "Getting listing at: {}", path );
                final String content =
                    contentController.renderListing( acceptInfo.getBaseAccept(), tk, path, baseUri, uriFormatter );

                response = formatOkResponseWithEntity( content, acceptInfo.getRawAccept() );
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
                        contentController.renderListing( acceptInfo.getBaseAccept(), tk, path + "/", baseUri,
                                                         uriFormatter );

                    response = formatOkResponseWithEntity( content, acceptInfo.getRawAccept() );
                }
                else
                {
                    final String contentType = contentController.getContentType( path );

                    item.touch();

                    response = Response.ok( new TransferStreamingOutput( item ) )
                                       .header( ApplicationHeader.content_type.key(), contentType )
                                       .build();
                }
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e, true );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e, true );
        }

        return response;
    }

}
