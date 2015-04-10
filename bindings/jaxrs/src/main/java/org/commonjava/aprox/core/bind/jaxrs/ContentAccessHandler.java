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
package org.commonjava.aprox.core.bind.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.core.ctl.ContentController.LISTING_HTML_FILE;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
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
import org.commonjava.aprox.core.bind.jaxrs.util.TransferStreamingOutput;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.util.HttpUtils;
import org.commonjava.aprox.util.AcceptInfo;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/{type: (hosted|group|remote)}/{name}" )
public class ContentAccessHandler
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentController contentController;

    @Inject
    private UriFormatter uriFormatter;

    @Inject
    private JaxRsRequestHelper jaxRsRequestHelper;

    public ContentAccessHandler()
    {
    }

    public ContentAccessHandler( final ContentController controller, final UriFormatter uriFormatter )
    {
        this.contentController = controller;
        this.uriFormatter = uriFormatter;
    }

    @PUT
    @Path( "/{path: (.+)?}" )
    public Response doCreate( final @PathParam( "type" ) String type, final @PathParam( "name" ) String name,
                              final @PathParam( "path" ) String path, final @Context UriInfo uriInfo,
                              final @Context HttpServletRequest request )
    {
        final StoreType st = StoreType.get( type );

        Response response = null;
        final Transfer transfer;
        try
        {
            transfer = contentController.store( new StoreKey( st, name ), path, request.getInputStream() );

            final StoreKey storageKey = LocationUtils.getKey( transfer );
            logger.info( "Key for storage location: {}", storageKey );

            final URI uri = uriInfo.getBaseUriBuilder()
                                   .path( getClass() )
                                   .path( path )
                                   .build( type, name );

            response = Response.created( uri )
                               .build();
        }
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( String.format( "Failed to upload: %s to: %s. Reason: %s", path, name, e.getMessage() ), e );

            response = formatResponse( e, true );
        }

        return response;
    }

    @DELETE
    @Path( "/{path: (.*)}" )
    public Response doDelete( final @PathParam( "type" ) String type, final @PathParam( "name" ) String name,
                              final @PathParam( "path" ) String path )
    {
        final StoreType st = StoreType.get( type );

        Response response;
        try
        {
            final ApplicationStatus result = contentController.delete( st, name, path );
            response = Response.status( result.code() )
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to delete artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    @HEAD
    @Path( "/{path: (.*)}" )
    public Response doHead( final @PathParam( "type" ) String type, final @PathParam( "name" ) String name,
                            final @PathParam( "path" ) String path, @Context final UriInfo uriInfo,
                            @Context final HttpServletRequest request )
    {
        final StoreType st = StoreType.get( type );
        final StoreKey sk = new StoreKey( st, name );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );

        Response response;
        try
        {
            final String baseUri = uriInfo.getBaseUriBuilder()
                                          .path( AproxDeployment.API_PREFIX )
                                          .build()
                                          .toString();


            if ( path == null || path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
            {
                logger.info( "Getting listing at: {}", path );
                final String content =
                    contentController.renderListing( acceptInfo.getBaseAccept(), sk, path, baseUri, uriFormatter );

                response =
                    Response.ok()
                            .header( ApplicationHeader.content_type.key(), acceptInfo.getRawAccept() )
                            .header( ApplicationHeader.content_length.key(), Long.toString( content.length() ) )
                            .header( ApplicationHeader.last_modified.key(), HttpUtils.formatDateHeader( new Date() ) )
                            .build();
            }
            else
            {
                final Transfer item = contentController.get( st, name, path );

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
    public Response doGet( final @PathParam( "type" ) String type, final @PathParam( "name" ) String name,
                           final @PathParam( "path" ) String path, @Context final UriInfo uriInfo,
                           @Context final HttpServletRequest request )
    {
        final StoreType st = StoreType.get( type );
        final StoreKey sk = new StoreKey( st, name );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );
        final String standardAccept = ApplicationContent.getStandardAccept( acceptInfo.getBaseAccept() );

        Response response;

        logger.info( "User asked for: {}\nStandard accept header for that is: {}", acceptInfo.getRawAccept(),
                     standardAccept );

        try
        {
            final String baseUri = uriInfo.getBaseUriBuilder()
                                          .path( AproxDeployment.API_PREFIX )
                                          .build()
                                          .toString();

            if ( path == null || path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
            {
                logger.info( "Getting listing at: {}", path );
                final String content =
                    contentController.renderListing( standardAccept, st, name, path, baseUri, uriFormatter );

                response = formatOkResponseWithEntity( content, acceptInfo.getRawAccept() );
            }
            else
            {
                final Transfer item = contentController.get( sk, path );

                if ( item.isDirectory()
                    || ( path.lastIndexOf( '.' ) < path.lastIndexOf( '/' ) && contentController.isHtmlContent( item ) ) )
                {
                    item.delete( false );

                    logger.info( "Getting listing at: {}", path + "/" );
                    final String content =
                        contentController.renderListing( standardAccept, st, name, path + "/", baseUri, uriFormatter );

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
        catch ( final AproxWorkflowException | IOException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e, true );
        }

        return response;
    }

}
