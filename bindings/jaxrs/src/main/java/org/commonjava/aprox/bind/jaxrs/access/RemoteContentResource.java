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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreType;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path( "/remote" )
@Api( description = "Handles GET/DELETE requests for content in a remote repository (proxy) store", value = "Handle remote repository content" )
@ApplicationScoped
public class RemoteContentResource
    extends AbstractContentResource<RemoteRepository>
{
    @DELETE
    @ApiOperation( value = "Delete content at the given path in repository's cache with the given name." )
    @ApiError( code = 404, reason = "If either the repository or the path within the repository doesn't exist" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response deleteContent( @ApiParam( "Name of the repository" ) @PathParam( "name" ) final String name,
                                   @ApiParam( "Content path within the repository" ) @PathParam( "path" ) final String path )
    {
        return doDelete( name, path );
    }

    @GET
    @ApiOperation( value = "Retrieve content given by path in repository with the given name." )
    @ApiError( code = 404, reason = "If either the repository or the path within the repository doesn't exist" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response getContent( @ApiParam( "Name of the repository" ) @PathParam( "name" ) final String name,
                                @ApiParam( "Content path within the repository" ) @PathParam( "path" ) final String path,
                                @Context final UriInfo uriInfo )
    {
        return doGet( name, path, uriInfo );
    }

    @Override
    protected StoreType getStoreType()
    {
        return StoreType.remote;
    }

}
