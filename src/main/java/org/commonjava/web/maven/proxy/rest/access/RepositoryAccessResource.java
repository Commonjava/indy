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
package org.commonjava.web.maven.proxy.rest.access;

import java.io.File;

import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.commonjava.auth.couch.model.Permission;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.maven.proxy.data.ProxyDataException;
import org.commonjava.web.maven.proxy.data.ProxyDataManager;
import org.commonjava.web.maven.proxy.model.ArtifactStore.StoreType;
import org.commonjava.web.maven.proxy.model.Repository;
import org.commonjava.web.maven.proxy.rest.util.Downloader;

@Path( "/repository" )
@RequestScoped
@RequiresAuthentication
public class RepositoryAccessResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyManager;

    @Inject
    private Downloader downloader;

    // @Context
    // private UriInfo uriInfo;

    @GET
    @Path( "/{name}{path: (/.+)?}" )
    public Response getContent( @PathParam( "name" ) final String name,
                                @PathParam( "path" ) final String path )
    {
        SecurityUtils.getSubject().isPermitted( Permission.name( StoreType.repository.name(), name,
                                                                 Permission.READ ) );

        // TODO:
        // 1. directory request (ends with "/")...browse somehow??
        // 2. empty path (directory request for proxy root)

        Repository proxy;

        try
        {
            proxy = proxyManager.getRepository( name );
            if ( proxy == null )
            {
                throw new WebApplicationException(
                                                   Response.status( Status.NOT_FOUND ).entity( "Proxy: "
                                                                                                   + name
                                                                                                   + " not found." ).build() );
            }
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to retrieve proxy: %s. Reason: %s", e, name, e.getMessage() );
            throw new WebApplicationException(
                                               Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
        }

        File target = downloader.download( proxy, path );

        String mimeType = new MimetypesFileTypeMap().getContentType( target );
        return Response.ok( target, mimeType ).build();
    }
}
