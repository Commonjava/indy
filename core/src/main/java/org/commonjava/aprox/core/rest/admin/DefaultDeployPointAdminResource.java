/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.rest.admin;

import static org.apache.commons.lang.StringUtils.join;

import java.util.List;

import javax.enterprise.context.RequestScoped;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.core.model.io.AProxModelSerializer;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.rest.admin.DeployPointAdminResource;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.model.Listing;

@Path( "/admin/deploys" )
@RequestScoped
public class DefaultDeployPointAdminResource
    implements DeployPointAdminResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager proxyManager;

    @Inject
    private AProxModelSerializer modelSerializer;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest request;

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#create()
     */
    @Override
    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    @Produces( MediaType.APPLICATION_JSON )
    public Response create()
    {
        final DeployPoint deploy = modelSerializer.deployPointFromRequestBody( request );

        logger.info( "\n\nGot proxy: %s\n\n", deploy );

        ResponseBuilder builder;
        try
        {
            if ( proxyManager.storeDeployPoint( deploy, true ) )
            {
                builder = Response.created( uriInfo.getAbsolutePathBuilder()
                                                   .path( deploy.getName() )
                                                   .build() )
                                  .entity( modelSerializer.toString( deploy ) );
            }
            else
            {
                builder = Response.status( Status.CONFLICT )
                                  .entity( "{\"error\": \"Deploy point already exists.\"}" );
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to create proxy: %s. Reason: %s", e, e.getMessage() );
            builder = Response.status( Status.INTERNAL_SERVER_ERROR );
        }

        return builder.build();
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#store(java.lang.String)
     */
    @Override
    @PUT
    @Path( "/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public Response store( @PathParam( "name" ) final String name )
    {
        final DeployPoint deploy = modelSerializer.deployPointFromRequestBody( request );

        ResponseBuilder builder;
        try
        {
            proxyManager.storeDeployPoint( deploy, false );
            builder = Response.created( uriInfo.getAbsolutePathBuilder()
                                               .build() );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to save proxy: %s. Reason: %s", e, e.getMessage() );
            builder = Response.status( Status.INTERNAL_SERVER_ERROR );
        }

        return builder.build();
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#getAll()
     */
    @Override
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response getAll()
    {
        try
        {
            final List<DeployPoint> deployPoints = proxyManager.getAllDeployPoints();
            logger.info( "Returning listing containing deploy points:\n\t%s", join( deployPoints, "\n\t" ) );

            final Listing<DeployPoint> listing = new Listing<DeployPoint>( deployPoints );

            final String json = modelSerializer.deployPointListingToString( listing );
            logger.info( "JSON:\n\n%s", json );

            return Response.ok()
                           .entity( json )
                           .build();
        }
        catch ( final ProxyDataException e )
        {
            logger.error( e.getMessage(), e );
            throw new WebApplicationException( Status.INTERNAL_SERVER_ERROR );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#get(java.lang.String)
     */
    @Override
    @GET
    @Path( "/{name}" )
    @Produces( MediaType.APPLICATION_JSON )
    public Response get( @PathParam( "name" ) final String name )
    {
        try
        {
            final DeployPoint deploy = proxyManager.getDeployPoint( name );
            logger.info( "Returning repository: %s", deploy );

            if ( deploy == null )
            {
                return Response.status( Status.NOT_FOUND )
                               .build();
            }
            else
            {
                return Response.ok()
                               .entity( modelSerializer.toString( deploy ) )
                               .build();
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( e.getMessage(), e );
            throw new WebApplicationException( Status.INTERNAL_SERVER_ERROR );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.DeployPointAdminResource#delete(java.lang.String)
     */
    @Override
    @DELETE
    @Path( "/{name}" )
    public Response delete( @PathParam( "name" ) final String name )
    {
        ResponseBuilder builder;
        try
        {
            proxyManager.deleteDeployPoint( name );
            builder = Response.ok();
        }
        catch ( final ProxyDataException e )
        {
            logger.error( e.getMessage(), e );
            builder = Response.status( Status.INTERNAL_SERVER_ERROR );
        }

        return builder.build();
    }

}
