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

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.io.StoreKeySerializer;
import org.commonjava.aprox.core.rest.admin.RepositoryAdminResource;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.common.model.Listing;
import org.commonjava.web.common.ser.JsonSerializer;

import com.google.gson.reflect.TypeToken;

@Path( "/admin/repository" )
@RequestScoped
public class DefaultRepositoryAdminResource
    implements RepositoryAdminResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyManager;

    @Inject
    private JsonSerializer restSerializer;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest request;

    @PostConstruct
    protected void registerSerializationAdapters()
    {
        restSerializer.registerSerializationAdapters( new StoreKeySerializer() );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.RepositoryAdminResource#create()
     */
    @Override
    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    public Response create()
    {
        @SuppressWarnings( "unchecked" )
        Repository repository = restSerializer.fromRequestBody( request, Repository.class );

        logger.info( "\n\nGot proxy: %s\n\n", repository );

        ResponseBuilder builder;
        try
        {
            if ( proxyManager.storeRepository( repository, true ) )
            {
                builder =
                    Response.created( uriInfo.getAbsolutePathBuilder().path( repository.getName() ).build() );
            }
            else
            {
                builder = Response.status( Status.CONFLICT ).entity( "Repository already exists." );
            }
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to create proxy: %s. Reason: %s", e, e.getMessage() );
            builder = Response.status( Status.INTERNAL_SERVER_ERROR );
        }

        return builder.build();
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.RepositoryAdminResource#store(java.lang.String)
     */
    @Override
    @POST
    @Path( "/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public Response store( @PathParam( "name" ) final String name )
    {
        @SuppressWarnings( "unchecked" )
        Repository repository = restSerializer.fromRequestBody( request, Repository.class );

        ResponseBuilder builder;
        try
        {
            Repository toUpdate = proxyManager.getRepository( name );
            if ( toUpdate == null )
            {
                toUpdate = repository;
            }
            else
            {
                toUpdate.setUrl( repository.getUrl() );
                toUpdate.setUser( repository.getUser() );
                toUpdate.setPassword( repository.getPassword() );
            }

            proxyManager.storeRepository( toUpdate );
            builder = Response.created( uriInfo.getAbsolutePathBuilder().build() );
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to save proxy: %s. Reason: %s", e, e.getMessage() );
            builder = Response.status( Status.INTERNAL_SERVER_ERROR );
        }

        return builder.build();
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.RepositoryAdminResource#getAll()
     */
    @Override
    @GET
    @Path( "/list" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Response getAll()
    {
        try
        {
            Listing<Repository> listing =
                new Listing<Repository>( proxyManager.getAllRepositories() );
            TypeToken<Listing<Repository>> tt = new TypeToken<Listing<Repository>>()
            {};

            return Response.ok().entity( restSerializer.toString( listing, tt.getType() ) ).build();
        }
        catch ( ProxyDataException e )
        {
            logger.error( e.getMessage(), e );
            throw new WebApplicationException( Status.INTERNAL_SERVER_ERROR );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.RepositoryAdminResource#get(java.lang.String)
     */
    @Override
    @GET
    @Path( "/{name}" )
    public Response get( @PathParam( "name" ) final String name )
    {
        try
        {
            Repository repo = proxyManager.getRepository( name );
            logger.info( "Returning repository: %s", repo );

            if ( repo == null )
            {
                return Response.status( Status.NOT_FOUND ).build();
            }
            else
            {
                return Response.ok().entity( restSerializer.toString( repo ) ).build();
            }
        }
        catch ( ProxyDataException e )
        {
            logger.error( e.getMessage(), e );
            throw new WebApplicationException( Status.INTERNAL_SERVER_ERROR );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.RepositoryAdminResource#delete(java.lang.String)
     */
    @Override
    @DELETE
    @Path( "/{name}" )
    public Response delete( @PathParam( "name" ) final String name )
    {
        ResponseBuilder builder;
        try
        {
            proxyManager.deleteRepository( name );
            builder = Response.ok();
        }
        catch ( ProxyDataException e )
        {
            logger.error( e.getMessage(), e );
            builder = Response.status( Status.INTERNAL_SERVER_ERROR );
        }

        return builder.build();
    }

}
