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
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.io.StorageItem;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.RESTWorkflowException;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.aprox.core.rest.util.retrieve.GroupHandlerChain;
import org.commonjava.util.logging.Logger;

@Path( "/group" )
@RequestScoped
public class DefaultGroupAccessResource
    implements GroupAccessResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager proxyManager;

    @Inject
    private GroupHandlerChain handlerChain;

    @Inject
    private FileManager fileManager;

    @Context
    private UriInfo uriInfo;

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#getProxyContent(java.lang.String,
     * java.lang.String)
     */
    @Override
    @GET
    @Path( "/{name}{path: (/.+)?}" )
    public Response getProxyContent( @PathParam( "name" ) final String name, @PathParam( "path" ) final String path )
    {
        // TODO:
        // 1. directory request (ends with "/")...browse somehow??
        // 2. empty path (directory request for proxy root)

        List<? extends ArtifactStore> stores = null;
        Group group = null;

        Response response = null;
        try
        {
            group = proxyManager.getGroup( name );
            if ( group == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                // logger.info( "Retrieving ordered stores for group: '%s'", name );
                stores = proxyManager.getOrderedConcreteStoresInGroup( name );
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        if ( response == null )
        {
            // logger.info( "Download: %s\nFrom: %s", path, stores );
            try
            {
                final StorageItem item = handlerChain.retrieve( group, stores, path );
                if ( item == null || item.isDirectory() )
                {
                    response = Response.status( Status.NOT_FOUND )
                                       .build();
                }
                else
                {
                    final String mimeType = new MimetypesFileTypeMap().getContentType( item.getPath() );
                    response = Response.ok( item.getStream(), mimeType )
                                       .build();
                }
            }
            catch ( final RESTWorkflowException e )
            {
                logger.error( "Failed to retrieve: %s from group: %s. Reason: %s", e, path, name, e.getMessage() );
                response = e.getResponse();
            }

        }

        return response;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#createContent(java.lang.String, java.lang.String,
     * javax.servlet.http.HttpServletRequest)
     */
    @Override
    @PUT
    @Path( "/{name}/{path: (.+)}" )
    public Response createContent( @PathParam( "name" ) final String name, @PathParam( "path" ) final String path,
                                   @Context final HttpServletRequest request )
    {
        List<? extends ArtifactStore> stores = null;
        Group group = null;

        Response response = null;
        try
        {
            group = proxyManager.getGroup( name );
            if ( group == null )
            {
                response = Response.status( Status.NOT_FOUND )
                                   .build();
            }
            else
            {
                stores = proxyManager.getOrderedConcreteStoresInGroup( name );
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve repository-group information: %s. Reason: %s", e, name, e.getMessage() );
            response = Response.serverError()
                               .build();
        }

        if ( response == null )
        {
            try
            {
                final DeployPoint deploy = handlerChain.store( group, stores, path, request.getInputStream() );

                response = Response.created( uriInfo.getAbsolutePathBuilder()
                                                    .path( deploy.getName() )
                                                    .path( path )
                                                    .build() )
                                   .build();
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to open stream from request: %s", e, e.getMessage() );
                response = Response.serverError()
                                   .build();
            }
            catch ( final RESTWorkflowException e )
            {
                logger.error( "Failed to upload: %s to group: %s. Reason: %s", e, path, name, e.getMessage() );
                response = e.getResponse();
            }
        }

        return response;
    }
}
