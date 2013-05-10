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
package org.commonjava.aprox.core.rest.access;

import java.io.IOException;

import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.access.GroupAccessResource;
import org.commonjava.aprox.rest.util.GroupContentManager;
import org.commonjava.util.logging.Logger;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path( "/group" )
@Api( description = "Handles GET/PUT/DELETE requests for content in the constituency of group store", value = "Handle group content" )
@RequestScoped
public class DefaultGroupAccessResource
    implements GroupAccessResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private GroupContentManager groupContentManager;

    @Inject
    private StoreDataManager dataManager;

    @Context
    private UriInfo uriInfo;

    @DELETE
    @ApiOperation( value = "Delete content at the given path from all constituent stores within the group with the given name." )
    @ApiError( code = 404, reason = "If the deletion fails" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response deleteContent( @ApiParam( "Name of the store" ) @PathParam( "name" ) final String name,
                                   @ApiParam( "Content path within the store" ) @PathParam( "path" ) final String path )
    {
        try
        {
            if ( groupContentManager.delete( name, path ) )
            {
                return Response.ok()
                               .build();
            }
            else
            {
                return Response.status( Status.NOT_FOUND )
                               .build();
            }

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return e.getResponse();
        }
        catch ( final IOException e )
        {
            logger.error( e.getMessage(), e );
            return Response.serverError()
                           .build();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#getProxyContent(java.lang.String,
     * java.lang.String)
     */
    @Override
    @GET
    @ApiOperation( value = "Retrieve content from the FIRST constituent store that contains the given path, within the group with the given name." )
    @ApiError( code = 404, reason = "If none of the constituent stores contains the path" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response getProxyContent( @ApiParam( "Name of the store" ) @PathParam( "name" ) final String name,
                                     @ApiParam( "Content path within the store" ) @PathParam( "path" ) final String path )
    {
        try
        {
            final StorageItem item = groupContentManager.retrieve( name, path );

            if ( item == null )
            {
                return Response.status( Status.NOT_FOUND )
                               .build();
            }

            final String mimeType = new MimetypesFileTypeMap().getContentType( item.getPath() );

            return Response.ok( item.openInputStream(), mimeType )
                           .build();

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return e.getResponse();
        }
        catch ( final IOException e )
        {
            logger.error( e.getMessage(), e );
            return Response.serverError()
                           .build();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#createContent(java.lang.String, java.lang.String,
     * javax.servlet.http.HttpServletRequest)
     */
    @Override
    @PUT
    @ApiOperation( value = "Store new content at the given path in the first deploy-point store constituent listed in the group with the given name." )
    @ApiError( code = 404, reason = "If the group doesn't contain any deploy-point stores" )
    @Path( "/{name}/{path: (.+)}" )
    public Response createContent( @ApiParam( "Name of the store" ) @PathParam( "name" ) final String name,
                                   @ApiParam( "Content path within the store" ) @PathParam( "path" ) final String path,
                                   @Context final HttpServletRequest request )
    {
        try
        {
            final StorageItem item = groupContentManager.store( name, path, request.getInputStream() );
            DeployPoint deploy;
            try
            {
                deploy = dataManager.getDeployPoint( item.getStoreKey()
                                                         .getName() );
            }
            catch ( final ProxyDataException e )
            {
                logger.error( e.getMessage(), e );
                return Response.serverError()
                               .build();
            }

            if ( deploy == null )
            {
                return Response.status( Status.NOT_FOUND )
                               .build();
            }

            return Response.created( uriInfo.getAbsolutePathBuilder()
                                            .path( deploy.getName() )
                                            .path( path )
                                            .build() )
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return e.getResponse();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to open stream from request: %s", e, e.getMessage() );
            return Response.serverError()
                           .build();
        }
    }
}
