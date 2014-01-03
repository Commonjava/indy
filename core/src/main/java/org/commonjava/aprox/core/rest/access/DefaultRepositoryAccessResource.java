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
package org.commonjava.aprox.core.rest.access;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.access.RepositoryAccessResource;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path( "/repository" )
@Api( description = "Handles GET/DELETE requests for content in a remote repository (proxy) store", value = "Handle repository content" )
@RequestScoped
public class DefaultRepositoryAccessResource
    extends AbstractSimpleAccessResource<Repository>
    implements RepositoryAccessResource
{
    @Inject
    private StoreDataManager proxyManager;

    // @Context
    // private UriInfo uriInfo;

    @Override
    protected Repository getArtifactStore( final String name )
        throws AproxWorkflowException
    {
        try
        {
            return proxyManager.getRepository( name );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve repository: %s. Reason: %s", e, name, e.getMessage() );
        }
    }

    @DELETE
    @ApiOperation( value = "Delete content at the given path in repository's cache with the given name." )
    @ApiError( code = 404, reason = "If either the repository or the path within the repository doesn't exist" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response deleteContent( @ApiParam( "Name of the repository" ) @PathParam( "name" ) final String name,
                                   @ApiParam( "Content path within the repository" ) @PathParam( "path" ) final String path )
    {
        return doDelete( name, path );
    }

    @Override
    @GET
    @ApiOperation( value = "Retrieve content given by path in repository with the given name." )
    @ApiError( code = 404, reason = "If either the repository or the path within the repository doesn't exist" )
    @Path( "/{name}{path: (/.+)?}" )
    public Response getContent( @ApiParam( "Name of the repository" ) @PathParam( "name" ) final String name,
                                @ApiParam( "Content path within the repository" ) @PathParam( "path" ) final String path )
    {
        return doGet( name, path );
    }

}
