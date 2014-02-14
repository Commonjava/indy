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
import javax.enterprise.inject.Default;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.StoreType;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path( "/hosted" )
@Api( description = "Handles GET/PUT/DELETE requests for content in a hosted repository", value = "Handle hosted repository content" )
@ApplicationScoped
@Default
public class HostedContentResource
    extends AbstractContentResource<HostedRepository>
{
    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.DeployPointAccessResource#createContent(java.lang.String,
     * java.lang.String, javax.servlet.http.HttpServletRequest)
     */
    @PUT
    @ApiOperation( value = "Store new content at the given path in store with the given name." )
    @Path( "/{name}/{path: (.+)}" )
    public Response createContent( @ApiParam( "Name of the store" ) @PathParam( "name" ) final String name,
                                   @ApiParam( "Content path within the store" ) @PathParam( "path" ) final String path,
                                   @Context final HttpServletRequest request, @Context final UriInfo uriInfo )
    {
        return doCreate( name, path, request, uriInfo );
    }

    @DELETE
    @ApiOperation( value = "Delete content at the given path in deploy-point with the given name." )
    @ApiError( code = 404, reason = "If either the deploy-point or the path within the deploy-point doesn't exist" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response deleteContent( @ApiParam( "Name of the hosted repository" ) @PathParam( "name" ) final String name,
                                   @ApiParam( "Content path within the hosted repository" ) @PathParam( "path" ) final String path )
    {
        return doDelete( name, path );
    }

    @GET
    @ApiOperation( value = "Retrieve content given by path in hosted repository with the given name." )
    @ApiError( code = 404, reason = "If either the hosted repository or the path within the hosted repository doesn't exist" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response getContent( @ApiParam( "Name of the deploy-point" ) @PathParam( "name" ) final String name,
                                @ApiParam( "Content path within the deploy-point" ) @PathParam( "path" ) final String path,
                                @Context final UriInfo uriInfo )
    {
        return doGet( name, path, uriInfo );
    }

    @Override
    protected StoreType getStoreType()
    {
        return StoreType.hosted;
    }

}
