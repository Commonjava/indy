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
package org.commonjava.aprox.core.rest.access;

import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
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
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.rest.access.DeployPointAccessResource;
import org.commonjava.util.logging.Logger;

@Path( "/deploy" )
@RequestScoped
@Default
public class DefaultDeployPointAccessResource
    extends AbstractSimpleAccessResource<DeployPoint>
    implements DeployPointAccessResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyManager;

    @Context
    private UriInfo uriInfo;

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.access.DeployPointAccessResource#createContent(java.lang.String,
     * java.lang.String, javax.servlet.http.HttpServletRequest)
     */
    @Override
    @PUT
    @Path( "/{name}/{path: (.+)}" )
    public Response createContent( @PathParam( "name" ) final String name,
                                   @PathParam( "path" ) final String path,
                                   @Context final HttpServletRequest request )
    {
        DeployPoint deploy = getArtifactStore( name );

        ResponseBuilder builder;
        try
        {
            getFileManager().upload( deploy, path, request.getInputStream() );

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

    @Override
    protected DeployPoint getArtifactStore( final String name )
    {
        try
        {
            return proxyManager.getDeployPoint( name );
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to retrieve deploy store: %s. Reason: %s", e, name,
                          e.getMessage() );
            throw new WebApplicationException(
                                               Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
        }
    }

}
