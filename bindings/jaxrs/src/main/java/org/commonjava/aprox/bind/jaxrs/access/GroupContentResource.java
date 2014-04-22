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
                                     @Context final UriInfo uriInfo )
    {
        return doGet( name, path, uriInfo );
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
