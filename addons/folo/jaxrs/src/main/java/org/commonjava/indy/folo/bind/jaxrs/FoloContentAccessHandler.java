/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.folo.bind.jaxrs;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponseFromMetadata;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.setInfoHeaders;
import static org.commonjava.indy.core.ctl.ContentController.LISTING_HTML_FILE;

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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyDeployment;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.JaxRsRequestHelper;
import org.commonjava.indy.core.bind.jaxrs.ContentAccessHandler;
import org.commonjava.indy.core.bind.jaxrs.util.TransferStreamingOutput;
import org.commonjava.indy.core.ctl.ContentController;
import org.commonjava.indy.folo.ctl.FoloConstants;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.util.HttpUtils;
import org.commonjava.indy.util.AcceptInfo;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.indy.util.UriFormatter;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modified copy of {@link ContentAccessHandler} that collects a tracking ID in addition to store type and name, then hands this off to
 * {@link ContentController} (with {@link EventMetadata} containing the tracking ID), which records artifact accesses.
 * 
 * NOTE: This is a result of copy/paste programming, so changes to {@link ContentAccessHandler} will have to be ported over.
 * 
 * @author jdcasey
 */
@Path( "/api/folo/track/{id}/{type: (hosted|group|remote)}/{name}" )
public class FoloContentAccessHandler
    implements IndyResources
{

    private static final String BASE_PATH = IndyDeployment.API_PREFIX + "/folo/track";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ContentController contentController;

    @Inject
    private UriFormatter uriFormatter;

    @Inject
    private JaxRsRequestHelper jaxRsRequestHelper;

    public FoloContentAccessHandler()
    {
    }

    public FoloContentAccessHandler( final ContentController controller, final UriFormatter uriFormatter,
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

        final TrackingKey tk = new TrackingKey( id );
        final StoreKey sk = new StoreKey( st, name );

        logger.info( "UPLOAD:\nPath: {}\nTracking key: {}\nStorage key: {}", path, tk, sk );

        Response response = null;
        Transfer transfer;
        try
        {
            final ServletInputStream inputStream = request.getInputStream();
            transfer =
                contentController.store( sk, path, inputStream,
                                         new EventMetadata().set( FoloConstants.TRACKING_KEY, tk ) );

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
            response = formatResponse( e );
        }
        catch ( final IndyWorkflowException e )
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

        final TrackingKey tk = new TrackingKey( id );
        final StoreKey sk = new StoreKey( st, name );

        logger.info( "EXISTS:\nPath: {}\nTracking key: {}\nStorage key: {}", path, tk, sk );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );

        Response response = null;
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
                final Transfer item =
                    contentController.get( sk, path, new EventMetadata().set( FoloConstants.TRACKING_KEY, tk ) );

                if ( item == null )
                {
                    if ( StoreType.remote == st )
                    {
                        final HttpExchangeMetadata metadata = contentController.getHttpMetadata( sk, path );
                        if ( metadata != null )
                        {
                            response = formatResponseFromMetadata( metadata );
                        }
                    }

                    if ( response == null )
                    {
                        response = Response.status( Status.NOT_FOUND )
                                           .build();
                    }
                }
                else
                {
                    final ResponseBuilder builder = Response.ok();
                    setInfoHeaders( builder, item, sk, path, false, contentController.getContentType( path ),
                                    contentController.getHttpMetadata( sk, path ) );
                    response = builder.build();
                }
            }
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e );
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

        final TrackingKey tk = new TrackingKey( id );
        final StoreKey sk = new StoreKey( st, name );

        logger.info( "DOWNLOAD:\nPath: {}\nTracking key: {}\nStorage key: {}", path, tk, sk );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );

        logger.info( "User asked for: {}\nStandard accept header for that is: {}", acceptInfo.getRawAccept(),
                     acceptInfo.getBaseAccept() );

        Response response = null;
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
                    contentController.renderListing( acceptInfo.getBaseAccept(), sk, path, baseUri, uriFormatter );

                response = formatOkResponseWithEntity( content, acceptInfo.getRawAccept() );
            }
            else
            {
                EventMetadata metadata = new EventMetadata().set( FoloConstants.TRACKING_KEY, tk );
                final Transfer item = contentController.get( sk, path, metadata );

                if ( item == null )
                {
                    if ( StoreType.remote == st )
                    {
                        final HttpExchangeMetadata httpMetadata = contentController.getHttpMetadata( sk, path );
                        if ( metadata != null )
                        {
                            response = formatResponseFromMetadata( httpMetadata );
                        }
                    }

                    if ( response == null )
                    {
                        response = Response.status( Status.NOT_FOUND )
                                           .build();
                    }
                }
                else if ( item.isDirectory()
                    || ( path.lastIndexOf( '.' ) < path.lastIndexOf( '/' ) && contentController.isHtmlContent( item ) ) )
                {
                    item.delete( false );

                    logger.info( "Getting listing at: {}", path + "/" );
                    final String content =
                        contentController.renderListing( acceptInfo.getBaseAccept(), sk, path + "/", baseUri,
                                                         uriFormatter );

                    response = formatOkResponseWithEntity( content, acceptInfo.getRawAccept() );
                }
                else
                {
                    final String contentType = contentController.getContentType( path );

                    item.touch( metadata );

                    response = Response.ok( new TransferStreamingOutput( item, metadata ) )
                                       .header( ApplicationHeader.content_type.key(), contentType )
                                       .build();
                }
            }
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e );
        }

        return response;
    }

}
