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
package org.commonjava.aprox.bind.jaxrs.admin;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.util.AproxExceptionUtils;
import org.commonjava.aprox.bind.jaxrs.util.ModelServletUtils;
import org.commonjava.aprox.core.rest.AdminController;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.AProxModelSerializer;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.web.json.model.Listing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/admin/{type}" )
@ApplicationScoped
public class AdminResource
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AdminController adminController;

    @Inject
    private AProxModelSerializer modelSerializer;

    @Inject
    private ModelServletUtils modelServletUtils;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest request;

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#create()
     */
    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    @Produces( MediaType.APPLICATION_JSON )
    public Response create( @PathParam( "type" ) final String type )
    {
        final StoreType st = StoreType.get( type );
        final ArtifactStore store = modelServletUtils.storeFromRequestBody( st, request );

        logger.info( "\n\nGot artifact store: {}\n\n", store );

        Response response;
        try
        {
            if ( adminController.store( store, true ) )
            {
                response = Response.created( uriInfo.getAbsolutePathBuilder()
                                                    .path( store.getName() )
                                                    .build() )
                                   .entity( modelSerializer.toString( store ) )
                                   .build();
            }
            else
            {
                response = Response.status( Status.CONFLICT )
                                   .entity( "{\"error\": \"Store already exists.\"}" )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to create artifact store: %s. Reason: %s", e.getMessage() ), e );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#store(java.lang.String)
     */
    @PUT
    @Path( "/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public Response store( @PathParam( "type" ) final String type, @PathParam( "name" ) final String name )
    {
        final StoreType st = StoreType.get( type );
        final ArtifactStore store = modelServletUtils.storeFromRequestBody( st, request );

        Response response;
        try
        {
            if ( adminController.store( store, false ) )
            {
                response = Response.created( uriInfo.getAbsolutePathBuilder()
                                                    .build() )
                                   .build();
            }
            else
            {
                response = Response.status( Status.NOT_MODIFIED )
                                   .entity( "{\"error\": \"Store changes not saved.\"}" )
                                   .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( String.format( "Failed to save proxy: %s. Reason: %s", e.getMessage() ), e );
            response = AproxExceptionUtils.formatResponse( e );
        }

        return response;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#getAll()
     */
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response getAll( @PathParam( "type" ) final String type )
    {
        final StoreType st = StoreType.get( type );

        try
        {
            @SuppressWarnings( "unchecked" )
            final List<ArtifactStore> stores = (List<ArtifactStore>) adminController.getAllOfType( st );
            logger.info( "Returning listing containing stores:\n\t{}", new JoinString( "\n\t", stores ) );

            final Listing<ArtifactStore> listing = new Listing<ArtifactStore>( stores );

            final String json = modelSerializer.storeListingToString( listing );
            logger.debug( "JSON:\n\n{}", json );

            return Response.ok()
                           .entity( json )
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#get(java.lang.String)
     */
    @GET
    @Path( "/{name}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response get( @PathParam( "type" ) final String type, @PathParam( "name" ) final String name )
    {
        final StoreType st = StoreType.get( type );
        final StoreKey key = new StoreKey( st, name );
        try
        {
            final ArtifactStore store = adminController.get( key );
            logger.info( "Returning repository: {}", store );

            if ( store == null )
            {
                return Response.status( Status.NOT_FOUND )
                               .build();
            }
            else
            {
                return Response.ok()
                               .entity( modelSerializer.toString( store ) )
                               .build();
            }
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#delete(java.lang.String)
     */
    @DELETE
    @Path( "/{name}" )
    public Response delete( @PathParam( "type" ) final String type, @PathParam( "name" ) final String name )
    {
        final StoreType st = StoreType.get( type );
        final StoreKey key = new StoreKey( st, name );

        try
        {
            adminController.delete( key );
            return Response.ok()
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return AproxExceptionUtils.formatResponse( e );
        }
    }

}
