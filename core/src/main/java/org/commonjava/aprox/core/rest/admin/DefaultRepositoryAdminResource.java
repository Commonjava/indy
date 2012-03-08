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
import org.commonjava.aprox.core.model.io.AProxModelSerializer;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.model.Listing;

@Path( "/admin/repository" )
@RequestScoped
public class DefaultRepositoryAdminResource
    implements RepositoryAdminResource
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyManager;

    @Inject
    private AProxModelSerializer modelSerializer;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest request;

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.admin.RepositoryAdminResource#create()
     */
    @Override
    @POST
    @Consumes( { MediaType.APPLICATION_JSON } )
    public Response create()
    {
        final Repository repository = modelSerializer.repositoryFromRequestBody( request );

        logger.info( "\n\nGot proxy: %s\n\n", repository );

        ResponseBuilder builder;
        try
        {
            if ( proxyManager.storeRepository( repository, true ) )
            {
                builder = Response.created( uriInfo.getAbsolutePathBuilder()
                                                   .path( repository.getName() )
                                                   .build() );
            }
            else
            {
                builder = Response.status( Status.CONFLICT )
                                  .entity( "Repository already exists." );
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
     * @see org.commonjava.aprox.core.rest.admin.RepositoryAdminResource#store(java.lang.String)
     */
    @Override
    @POST
    @Path( "/{name}" )
    @Consumes( { MediaType.APPLICATION_JSON } )
    public Response store( @PathParam( "name" ) final String name )
    {
        final Repository repository = modelSerializer.repositoryFromRequestBody( request );
        logger.info( "Storing changes to repository: %s", repository );

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
                toUpdate.setTimeoutSeconds( repository.getTimeoutSeconds() );
                toUpdate.setCacheTimeoutSeconds( repository.getCacheTimeoutSeconds() );
            }

            final boolean result = proxyManager.storeRepository( toUpdate, false );
            logger.info( "Repository: %s updated? %s", repository.getName(), result );
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
            final List<Repository> repos = proxyManager.getAllRepositories();
            logger.info( "Returning listing containing repositories:\n\t%s", join( repos, "\n\t" ) );

            final Listing<Repository> listing = new Listing<Repository>( repos );
            logger.info( "Listing:\n\n%s", listing );

            // final String json = serializer.toString( listing, new TypeToken<Listing<Repository>>()
            // {
            // }.getType() );

            final String json = modelSerializer.repoListingToString( listing );

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
     * @see org.commonjava.aprox.core.rest.admin.RepositoryAdminResource#get(java.lang.String)
     */
    @Override
    @GET
    @Path( "/{name}" )
    public Response get( @PathParam( "name" ) final String name )
    {
        try
        {
            final Repository repo = proxyManager.getRepository( name );
            logger.info( "Returning repository: %s for name: %s", repo, name );

            if ( repo == null )
            {
                return Response.status( Status.NOT_FOUND )
                               .build();
            }
            else
            {
                return Response.ok()
                               .entity( modelSerializer.toString( repo ) )
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
        catch ( final ProxyDataException e )
        {
            logger.error( e.getMessage(), e );
            builder = Response.status( Status.INTERNAL_SERVER_ERROR );
        }

        return builder.build();
    }

}
