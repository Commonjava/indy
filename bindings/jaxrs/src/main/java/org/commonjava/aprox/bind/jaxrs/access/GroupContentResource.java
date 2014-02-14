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
package org.commonjava.aprox.bind.jaxrs.access;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreType;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path( "/group" )
@Api( description = "Handles GET/PUT/DELETE requests for content in the constituency of group store", value = "Handle group content" )
@ApplicationScoped
public class GroupContentResource
    extends AbstractContentResource<Group>
{

    @DELETE
    @ApiOperation( value = "Delete content at the given path from all constituent stores within the group with the given name." )
    @ApiError( code = 404, reason = "If the deletion fails" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response deleteContent( @ApiParam( "Name of the store" ) @PathParam( "name" ) final String name,
                                   @ApiParam( "Content path within the store" ) @PathParam( "path" ) final String path )
    {
        return doDelete( name, path );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#getProxyContent(java.lang.String,
     * java.lang.String)
     */
    @GET
    @ApiOperation( value = "Retrieve content from the FIRST constituent store that contains the given path, within the group with the given name." )
    @ApiError( code = 404, reason = "If none of the constituent stores contains the path" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response getProxyContent( @ApiParam( "Name of the store" ) @PathParam( "name" ) final String name,
                                     @ApiParam( "Content path within the store" ) @PathParam( "path" ) final String path,
                                     @Context final UriBuilder uriBuilder )
    {
        return doGet( name, path, uriBuilder );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#createContent(java.lang.String, java.lang.String,
     * javax.servlet.http.HttpServletRequest)
     */
    @PUT
    @ApiOperation( value = "Store new content at the given path in the first deploy-point store constituent listed in the group with the given name." )
    @ApiError( code = 404, reason = "If the group doesn't contain any deploy-point stores" )
    @Path( "/{name}/{path: (.+)}" )
    public Response createContent( @ApiParam( "Name of the store" ) @PathParam( "name" ) final String name,
                                   @ApiParam( "Content path within the store" ) @PathParam( "path" ) final String path,
                                   @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        return doCreate( name, path, request, uriInfo );
    }

    @Override
    protected StoreType getStoreType()
    {
        return StoreType.group;
    }
}
