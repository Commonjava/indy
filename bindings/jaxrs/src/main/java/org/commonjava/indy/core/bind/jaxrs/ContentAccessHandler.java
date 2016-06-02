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
package org.commonjava.indy.core.bind.jaxrs;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponseFromMetadata;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.setInfoHeaders;
import static org.commonjava.indy.core.ctl.ContentController.LISTING_HTML_FILE;

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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyDeployment;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.bind.jaxrs.util.JaxRsRequestHelper;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.core.bind.jaxrs.util.TransferStreamingOutput;
import org.commonjava.indy.core.ctl.ContentController;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.util.HttpUtils;
import org.commonjava.indy.util.AcceptInfo;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.indy.util.UriFormatter;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api( value = "/<type>/<name>", description = "Handles retrieval and management of file/artifact content. This is the main point of access for most users." )
@Path( "/api/{type: (hosted|group|remote)}/{name}" )
public class ContentAccessHandler
    implements IndyResources
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

    @ApiOperation( "Store file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( {
        @ApiResponse( code = 201, message = "Content was stored successfully" ),
        @ApiResponse( code = 400, message = "No appropriate storage location was found in the specified store (this store, or a member if a group is specified)." ) } )
    @PUT
    @Path( "/{path: (.+)?}" )
    public Response doCreate( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                              final @ApiParam( required = true ) @PathParam( "name" ) String name,
                              final @PathParam( "path" ) String path, final @Context UriInfo uriInfo,
                              final @Context HttpServletRequest request )
    {
        final StoreType st = StoreType.get( type );
        StoreKey sk = new StoreKey( st, name );

        EventMetadata eventMetadata = new EventMetadata().set( ContentManager.ENTRY_POINT_STORE, sk );

        Response response = null;
        final Transfer transfer;
        try
        {
            transfer = contentController.store( new StoreKey( st, name ), path, request.getInputStream(), eventMetadata );

            final StoreKey storageKey = LocationUtils.getKey( transfer );
            logger.info( "Key for storage location: {}", storageKey );

            final URI uri = uriInfo.getBaseUriBuilder()
                                   .path( getClass() )
                                   .path( path )
                                   .build( type, name );

            response = Response.created( uri )
                               .build();
        }
        catch ( final IndyWorkflowException | IOException e )
        {
            logger.error( String.format( "Failed to upload: %s to: %s. Reason: %s", path, name, e.getMessage() ), e );

            response = formatResponse( e );
        }

        return response;
    }

    @ApiOperation( "Delete file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 204, message = "Content was deleted successfully" ) } )
    @DELETE
    @Path( "/{path: (.*)}" )
    public Response doDelete( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                              final @ApiParam( required = true ) @PathParam( "name" ) String name,
                              final @PathParam( "path" ) String path )
    {
        final StoreType st = StoreType.get( type );
        StoreKey sk = new StoreKey( st, name );

        EventMetadata eventMetadata = new EventMetadata().set( ContentManager.ENTRY_POINT_STORE, sk );

        Response response;
        try
        {
            final ApplicationStatus result = contentController.delete( st, name, path, eventMetadata );
            response = Response.status( result.code() ).build();
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to delete artifact: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e );
        }
        return response;
    }

    @ApiOperation( "Store file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( { @ApiResponse( code = 200, message = "Header metadata for content (or rendered listing when path ends with '/index.html' or '/'" ), } )
    @HEAD
    @Path( "/{path: (.*)}" )
    public Response doHead( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                            final @ApiParam( required = true ) @PathParam( "name" ) String name,
                            final @PathParam( "path" ) String path, @Context final UriInfo uriInfo,
                            @Context final HttpServletRequest request )
    {
        final StoreType st = StoreType.get( type );
        final StoreKey sk = new StoreKey( st, name );

        EventMetadata eventMetadata = new EventMetadata().set( ContentManager.ENTRY_POINT_STORE, sk );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );

        Response response = null;
        final String baseUri = uriInfo.getBaseUriBuilder()
                                      .path( IndyDeployment.API_PREFIX )
                                      .build()
                                      .toString();

        if ( path == null || path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
        {
            try
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
            catch ( final IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to list content: %s from: %s. Reason: %s", path, name,
                                             e.getMessage() ), e );
                response = formatResponse( e );
            }
        }
        else
        {
            try
            {
                final Transfer item = contentController.get( sk, path, eventMetadata );
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
                    setInfoHeaders( builder, item, sk, path, true, contentController.getContentType( path ),
                                    contentController.getHttpMetadata( sk, path ) );

                    response = builder.build();
                }
            }
            catch ( final IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                             e.getMessage() ), e );
                response = formatResponse( e );
            }
        }
        return response;
    }

    @ApiOperation( "Retrieve file/artifact content under the given artifact store (type/name) and path." )
    @ApiResponses( {
        @ApiResponse( code = 200, response = String.class, message = "Rendered content listing (when path ends with '/index.html' or '/')" ),
        @ApiResponse( code = 200, response = StreamingOutput.class, message = "Content stream" ), } )
    @GET
    @Path( "/{path: (.*)}" )
    public Response doGet( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                           final @ApiParam( required = true ) @PathParam( "name" ) String name,
                           final @PathParam( "path" ) String path, @Context final UriInfo uriInfo,
                           @Context final HttpServletRequest request )
    {
        final StoreType st = StoreType.get( type );
        final StoreKey sk = new StoreKey( st, name );

        EventMetadata eventMetadata = new EventMetadata().set( ContentManager.ENTRY_POINT_STORE, sk );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );
        final String standardAccept = ApplicationContent.getStandardAccept( acceptInfo.getBaseAccept() );

        Response response = null;

        logger.info( "GET path: '{}' (RAW: '{}')\nIn store: '{}'\nUser accept header is: '{}'\nStandard accept header for that is: '{}'", path, request.getPathInfo(), sk, acceptInfo.getRawAccept(),
                     standardAccept );

        final String baseUri = uriInfo.getBaseUriBuilder()
                                      .path( IndyDeployment.API_PREFIX )
                                      .build()
                                      .toString();

        if ( path == null || path.equals( "" ) || request.getPathInfo().endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
        {
            try
            {
                logger.info( "Getting listing at: {}", path );
                final String content =
                    contentController.renderListing( standardAccept, st, name, path, baseUri, uriFormatter );

                response = formatOkResponseWithEntity( content, acceptInfo.getRawAccept() );
            }
            catch ( final IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to render content listing: %s from: %s. Reason: %s", path, name,
                                             e.getMessage() ), e );
                response = formatResponse( e );
            }
        }
        else
        {
            try
            {
                final Transfer item = contentController.get( sk, path );

                if ( item == null )
                {
                    if ( StoreType.remote == st )
                    {
                        try
                        {
                            final HttpExchangeMetadata metadata = contentController.getHttpMetadata( sk, path );
                            if ( metadata != null )
                            {
                                response = formatResponseFromMetadata( metadata );
                            }
                        }
                        catch ( final IndyWorkflowException e )
                        {
                            logger.error( String.format( "Error retrieving status metadata for: %s from: %s. Reason: %s",
                                                         path, name, e.getMessage() ), e );
                            response = formatResponse( e );
                        }
                    }

                    if ( response == null )
                    {
                        response = Response.status( Status.NOT_FOUND )
                                           .build();
                    }
                }
                else if ( item.isDirectory() || ( path.endsWith( "index.html" ) ) )
                {
                    try
                    {
                        item.delete( false );

                        logger.info( "Getting listing at: {}", path + "/" );
                        final String content =
                            contentController.renderListing( standardAccept, st, name, path + "/", baseUri,
                                                             uriFormatter );

                        response = formatOkResponseWithEntity( content, acceptInfo.getRawAccept() );
                    }
                    catch ( final IndyWorkflowException | IOException e )
                    {
                        logger.error( String.format( "Failed to render content listing: %s from: %s. Reason: %s", path,
                                                     name, e.getMessage() ), e );
                        response = formatResponse( e );
                    }
                }
                else
                {
                    item.touch();

                    final ResponseBuilder builder = Response.ok( new TransferStreamingOutput( item ,new EventMetadata() ) );
                    setInfoHeaders( builder, item, sk, path, false, contentController.getContentType( path ),
                                    contentController.getHttpMetadata( sk, path ) );

                    response = builder.build();
                }
            }
            catch ( final IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                             e.getMessage() ), e );
                response = formatResponse( e );
            }
        }

        return response;
    }

    @ApiOperation( "Retrieve root listing under the given artifact store (type/name)." )
    @ApiResponses( { @ApiResponse( code = 200, response = String.class,
                                   message = "Rendered root content listing" ),
                           @ApiResponse( code = 200, response = StreamingOutput.class, message = "Content stream" ), } )
    @GET
    @Path( "/" )
    public Response doGet(
            final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" )
            String type, final @ApiParam( required = true ) @PathParam( "name" ) String name,
            @Context final UriInfo uriInfo, @Context final HttpServletRequest request )
    {
        return doGet( type, name, "", uriInfo, request );
    }

}
