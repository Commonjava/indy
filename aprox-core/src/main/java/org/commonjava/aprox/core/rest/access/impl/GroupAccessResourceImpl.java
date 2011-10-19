/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.rest.access.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.rest.access.GroupAccessResource;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.aprox.core.rest.util.retrieve.GroupRetrieverChain;
import org.commonjava.util.logging.Logger;

@Path( "/group" )
@RequestScoped
public class GroupAccessResourceImpl
    implements GroupAccessResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyManager;

    @Inject
    private GroupRetrieverChain handlerChain;

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
    public Response getProxyContent( @PathParam( "name" ) final String name,
                                     @PathParam( "path" ) final String path )
    {
        // TODO:
        // 1. directory request (ends with "/")...browse somehow??
        // 2. empty path (directory request for proxy root)

        List<ArtifactStore> stores;
        Group group;

        try
        {
            group = proxyManager.getGroup( name );
            if ( group == null )
            {
                throw new WebApplicationException(
                                                   Response.status( Status.NOT_FOUND ).entity( "Repository group: "
                                                                                                   + name
                                                                                                   + " not found." ).build() );
            }

            // logger.info( "Retrieving ordered stores for group: '%s'", name );
            stores = proxyManager.getOrderedConcreteStoresInGroup( name );
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to retrieve repository-group information: %s. Reason: %s", e,
                          name, e.getMessage() );
            throw new WebApplicationException(
                                               Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
        }

        // logger.info( "Download: %s\nFrom: %s", path, stores );
        File target = handlerChain.retrieve( group, stores, path );

        if ( target == null )
        {
            return Response.status( Status.NOT_FOUND ).build();
        }

        String mimeType = new MimetypesFileTypeMap().getContentType( target );
        return Response.ok( target, mimeType ).build();
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.GroupAccessResource#createContent(java.lang.String, java.lang.String,
     * javax.servlet.http.HttpServletRequest)
     */
    @Override
    @PUT
    @Path( "/{name}/{path: (.+)}" )
    public Response createContent( @PathParam( "name" ) final String name,
                                   @PathParam( "path" ) final String path,
                                   @Context final HttpServletRequest request )
    {
        List<ArtifactStore> stores;
        Group group;

        try
        {
            group = proxyManager.getGroup( name );
            if ( group == null )
            {
                throw new WebApplicationException(
                                                   Response.status( Status.NOT_FOUND ).entity( "Repository group: "
                                                                                                   + name
                                                                                                   + " not found." ).build() );
            }

            stores = proxyManager.getOrderedConcreteStoresInGroup( name );
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to retrieve repository-group information: %s. Reason: %s", e,
                          name, e.getMessage() );
            throw new WebApplicationException(
                                               Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
        }

        List<DeployPoint> deployPoints = new ArrayList<DeployPoint>();
        if ( stores != null )
        {
            for ( ArtifactStore store : stores )
            {
                logger.info( "Looking for deploy-points...processing: %s", store.getKey() );
                if ( store instanceof DeployPoint )
                {
                    deployPoints.add( (DeployPoint) store );
                }
            }
        }

        ResponseBuilder builder;
        try
        {
            DeployPoint deploy = fileManager.upload( deployPoints, path, request.getInputStream() );

            builder =
                Response.created( uriInfo.getAbsolutePathBuilder().path( deploy.getName() ).path( path ).build() );
        }
        catch ( IOException e )
        {
            logger.error( "Failed to open stream from request: %s", e, e.getMessage() );
            builder = Response.status( Status.INTERNAL_SERVER_ERROR );
        }

        return builder.build();
    }
}
