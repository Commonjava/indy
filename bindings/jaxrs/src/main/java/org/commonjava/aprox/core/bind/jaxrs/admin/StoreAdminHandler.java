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
package org.commonjava.aprox.core.bind.jaxrs.admin;

import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.notModified;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatCreatedResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.aprox.model.core.ArtifactStore.METADATA_CHANGELOG;
import static org.commonjava.aprox.util.ApplicationContent.application_json;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.SecurityManager;
import org.commonjava.aprox.core.ctl.AdminController;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.StoreListingDTO;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Api( description = "Resource for accessing and managing artifact store definitions", value = "/api/admin/<type>" )
@Path( "/api/admin/{type}" )
public class StoreAdminHandler
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AdminController adminController;

    @Inject
    private AproxObjectMapper objectMapper;

    @Inject
    private SecurityManager securityManager;

    public StoreAdminHandler()
    {
        logger.info( "\n\n\n\nStarted StoreAdminHandler\n\n\n\n" );
    }

    //    @Context
    //    private UriInfo uriInfo;
    //
    //    @Context
    //    private HttpServletRequest request;

    @ApiOperation( "Check if a given store exists" )
    @ApiResponses( { @ApiResponse( code = 200, message = "The store exists" ),
        @ApiResponse( code = 404, message = "The store doesn't exist" ) } )
    @Path( "/{name}" )
    @HEAD
    public Response exists( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                            @ApiParam( required = true ) @PathParam( "name" ) final String name )
    {
        Response response;
        final StoreType st = StoreType.get( type );

        logger.info( "Checking for existence of: {}:{}", st, name );

        if ( adminController.exists( new StoreKey( st, name ) ) )
        {

            logger.info( "returning OK" );
            response = Response.ok()
                               .build();
        }
        else
        {
            logger.info( "Returning NOT FOUND" );
            response = Response.status( Status.NOT_FOUND )
                               .build();
        }
        return response;
    }

    @ApiOperation( "Create a new store" )
    @ApiResponses( { @ApiResponse( code = 201, response = ArtifactStore.class, message = "The store was created" ),
        @ApiResponse( code = 409, message = "A store with the specified type and name already exists" ) } )
    @ApiImplicitParams( { @ApiImplicitParam( allowMultiple = false, paramType = "body", name = "body", required = true, dataType = "org.commonjava.aprox.model.core.ArtifactStore", value = "The artifact store definition JSON" ) } )
    @POST
    @Consumes( ApplicationContent.application_json )
    @Produces( ApplicationContent.application_json )
    public Response create( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                            final @Context UriInfo uriInfo,
                            final @Context HttpServletRequest request,
                            final @Context SecurityContext securityContext )
    {
        final StoreType st = StoreType.get( type );

        Response response = null;
        String json = null;
        try
        {
            json = IOUtils.toString( request.getInputStream() );
            json = objectMapper.patchLegacyStoreJson( json );
        }
        catch ( final IOException e )
        {
            final String message = "Failed to read " + st.getStoreClass()
                                                         .getSimpleName() + " from request body.";

            logger.error( message, e );
            response = formatResponse( e, message );
        }

        if ( response != null )
        {
            return response;
        }

        ArtifactStore store = null;
        try
        {
            store = objectMapper.readValue( json, st.getStoreClass() );
        }
        catch ( final IOException e )
        {
            final String message = "Failed to parse " + st.getStoreClass()
                                                         .getSimpleName() + " from request body.";

            logger.error( message, e );
            response = formatResponse( e, message );
        }

        if ( response != null )
        {
            return response;
        }

        logger.info( "\n\nGot artifact store: {}\n\n", store );

        try
        {
            String user = securityManager.getUser( securityContext, request );

            if ( adminController.store( store, user, true ) )
            {
                final URI uri = uriInfo.getBaseUriBuilder()
                                       .path( getClass() )
                                       .path( store.getName() )
                                       .build( store.getKey().getType().singularEndpointName() );

                response = formatCreatedResponseWithJsonEntity( uri, store, objectMapper );
            }
            else
            {
                response = status( CONFLICT )
                                   .entity( "{\"error\": \"Store already exists.\"}" )
                                   .type( application_json )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }
        return response;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#store(java.lang.String)
     */
    @ApiOperation( "Update an existing store" )
    @ApiResponses( { @ApiResponse( code = 200, message = "The store was updated" ),
        @ApiResponse( code = 400, message = "The store specified in the body JSON didn't match the URL parameters" ), } )
    @ApiImplicitParams( { @ApiImplicitParam( allowMultiple = false, paramType = "body", name = "body", required = true, dataType = "org.commonjava.aprox.model.core.ArtifactStore", value = "The artifact store definition JSON" ) } )
    @Path( "/{name}" )
    @PUT
    @Consumes( ApplicationContent.application_json )
    public Response store( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                           final @ApiParam( required = true ) @PathParam( "name" ) String name,
                           final @Context HttpServletRequest request,
                           final @Context SecurityContext securityContext )
    {
        final StoreType st = StoreType.get( type );

        Response response = null;
        String json = null;
        try
        {
            json = IOUtils.toString( request.getInputStream() );
            json = objectMapper.patchLegacyStoreJson( json );
        }
        catch ( final IOException e )
        {
            final String message = "Failed to read " + st.getStoreClass()
                                                         .getSimpleName() + " from request body.";

            logger.error( message, e );
            response = formatResponse( e, message );
        }

        if ( response != null )
        {
            return response;
        }

        ArtifactStore store = null;
        try
        {
            store = objectMapper.readValue( json, st.getStoreClass() );
        }
        catch ( final IOException e )
        {
            final String message = "Failed to parse " + st.getStoreClass()
                                                          .getSimpleName() + " from request body.";

            logger.error( message, e );
            response = formatResponse( e, message );
        }

        if ( response != null )
        {
            return response;
        }

        if ( !name.equals( store.getName() ) )
        {
            response =
                Response.status( Status.BAD_REQUEST )
                        .entity( String.format( "Store in URL path is: '%s' but in JSON it is: '%s'", name,
                                                store.getName() ) )
                        .build();
        }

        try
        {
            String user = securityManager.getUser( securityContext, request );

            logger.info( "Storing: {}", store );
            if ( adminController.store( store, user, false ) )
            {
                response = ok().build();
            }
            else
            {
                logger.warn( "{} NOT modified!", store );
                response = notModified().build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }

        return response;
    }

    @ApiOperation( "Retrieve the definitions of all artifact stores of a given type on the system" )
    @ApiResponses( { @ApiResponse( code = 200, response = StoreListingDTO.class, message = "The store definitions" ), } )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAll( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type )
    {
        final StoreType st = StoreType.get( type );

        Response response;
        try
        {
            @SuppressWarnings( "unchecked" )
            final List<ArtifactStore> stores = (List<ArtifactStore>) adminController.getAllOfType( st );

            logger.info( "Returning listing containing stores:\n\t{}", new JoinString( "\n\t", stores ) );

            final StoreListingDTO<ArtifactStore> dto = new StoreListingDTO<ArtifactStore>( stores );

            response = formatOkResponseWithJsonEntity( dto, objectMapper );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }

        return response;
    }

    @ApiOperation( "Retrieve the definition of a specific artifact store" )
    @ApiResponses( { @ApiResponse( code = 200, response = ArtifactStore.class, message = "The store definition" ),
        @ApiResponse( code = 404, message = "The store doesn't exist" ), } )
    @Path( "/{name}" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response get( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                         final @ApiParam( required = true ) @PathParam( "name" ) String name )
    {
        final StoreType st = StoreType.get( type );
        final StoreKey key = new StoreKey( st, name );

        Response response;
        try
        {
            final ArtifactStore store = adminController.get( key );
            logger.info( "Returning repository: {}", store );

            if ( store == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                response = formatOkResponseWithJsonEntity( store, objectMapper );
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }
        return response;
    }

    @ApiOperation( "Delete an artifact store" )
    @ApiResponses( { @ApiResponse( code = 204, response = ArtifactStore.class, message = "The store was deleted (or didn't exist in the first place)" ), } )
    @Path( "/{name}" )
    @DELETE
    public Response delete( final @ApiParam( allowableValues = "hosted,group,remote", required = true ) @PathParam( "type" ) String type,
                            final @ApiParam( required = true ) @PathParam( "name" ) String name,
                            @Context final HttpServletRequest request,
                            final @Context SecurityContext securityContext )
    {
        final StoreType st = StoreType.get( type );
        final StoreKey key = new StoreKey( st, name );

        logger.info( "Deleting: {}", key );
        Response response;
        try
        {
            String summary = null;
            try
            {
                summary = IOUtils.toString( request.getInputStream() );
            }
            catch ( final IOException e )
            {
                // no problem, try to get the summary from a header instead.
                logger.info( "store-deletion change summary not in request body, checking headers." );
            }

            if ( isEmpty( summary ) )
            {
                summary = request.getHeader( METADATA_CHANGELOG );
            }

            if ( isEmpty( summary ) )
            {
                summary = "Changelog not provided";
            }

            String user = securityManager.getUser( securityContext, request );

            adminController.delete( key, user, summary );

            response = noContent().build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e );
        }
        return response;
    }

}
