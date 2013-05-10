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
            throw new AproxWorkflowException( Response.serverError()
                                                      .build(), "Failed to retrieve repository: %s. Reason: %s", e,
                                              name, e.getMessage() );
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
