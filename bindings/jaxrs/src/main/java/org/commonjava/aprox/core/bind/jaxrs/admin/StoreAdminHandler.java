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

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatCreatedResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatResponse;

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
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.util.SecurityParam;
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

@Path( "/api/admin/{type}" )
public class StoreAdminHandler
    implements AproxResources
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AdminController adminController;

    @Inject
    private AproxObjectMapper objectMapper;

    public StoreAdminHandler()
    {
        logger.info( "\n\n\n\nStarted StoreAdminHandler\n\n\n\n" );
    }

    //    @Context
    //    private UriInfo uriInfo;
    //
    //    @Context
    //    private HttpServletRequest request;

    @Path( "/{name}" )
    @HEAD
    public Response exists( final @PathParam( "type" ) String type, @PathParam( "name" ) final String name )
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

    @POST
    @Consumes( ApplicationContent.application_json )
    @Produces( ApplicationContent.application_json )
    public Response create( final @PathParam( "type" ) String type, final @Context UriInfo uriInfo,
                            final @Context HttpServletRequest request )
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
            response = formatResponse( e, message, true );
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
            response = formatResponse( e, message, true );
        }

        if ( response != null )
        {
            return response;
        }

        logger.info( "\n\nGot artifact store: {}\n\n", store );

        try
        {
            final String user = (String) request.getSession( true )
                                                .getAttribute( SecurityParam.user.key() );

            if ( adminController.store( store, user, true ) )
            {
                final URI uri = uriInfo.getBaseUriBuilder()
                                       .path( getClass() )
                                       .path( store.getName() )
                                       .build( store.getKey()
                                                    .getType()
                                                    .singularEndpointName() );

                response = formatCreatedResponseWithJsonEntity( uri, store, objectMapper );
            }
            else
            {
                response = Response.status( Status.CONFLICT )
                                   .entity( "{\"error\": \"Store already exists.\"}" )
                                   .type( ApplicationContent.application_json )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#store(java.lang.String)
     */
    @Path( "/{name}" )
    @PUT
    @Consumes( ApplicationContent.application_json )
    public Response store( final @PathParam( "type" ) String type, @PathParam( "name" ) final String name,
                           final @Context HttpServletRequest request )
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
            response = formatResponse( e, message, true );
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
            response = formatResponse( e, message, true );
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
            final String user = (String) request.getSession( true )
                                                .getAttribute( SecurityParam.user.key() );

            if ( adminController.store( store, user, false ) )
            {
                response = Response.ok()
                                   .build();
            }
            else
            {
                response = Response.notModified()
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }

        return response;
    }

    @GET
    @Produces( ApplicationContent.application_json )
    public Response getAll( final @PathParam( "type" ) String type )
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
            response = formatResponse( e, true );
        }

        return response;
    }

    @Path( "/{name}" )
    @GET
    @Produces( ApplicationContent.application_json )
    public Response get( final @PathParam( "type" ) String type, @PathParam( "name" ) final String name )
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
            response = formatResponse( e, true );
        }
        return response;
    }

    @Path( "/{name}" )
    @DELETE
    public Response delete( final @PathParam( "type" ) String type, @PathParam( "name" ) final String name,
                            @Context final HttpServletRequest request )
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
                summary = request.getHeader( ArtifactStore.METADATA_CHANGELOG );
            }

            if ( isEmpty( summary ) )
            {
                summary = "Changelog not provided";
            }

            final String user = (String) request.getSession( true )
                                                .getAttribute( SecurityParam.user.key() );

            adminController.delete( key, user, summary );

            response = Response.noContent()
                               .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            response = formatResponse( e, true );
        }
        return response;
    }

}
