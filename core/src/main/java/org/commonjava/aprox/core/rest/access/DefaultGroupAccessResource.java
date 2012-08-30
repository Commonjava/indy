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

import org.commonjava.aprox.core.io.StorageItem;
import org.commonjava.aprox.core.model.DeployPoint;
import org.commonjava.aprox.core.rest.AproxWorkflowException;
import org.commonjava.aprox.core.rest.util.GroupContentManager;
import org.commonjava.util.logging.Logger;

@Path( "/group" )
@RequestScoped
public class DefaultGroupAccessResource
    implements GroupAccessResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private GroupContentManager groupContentManager;

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
        try
        {
            final StorageItem item = groupContentManager.retrieve( name, path );

            if ( item == null )
            {
                return Response.status( Status.NOT_FOUND )
                               .build();
            }

            final String mimeType = new MimetypesFileTypeMap().getContentType( item.getPath() );

            return Response.ok( item.getStream(), mimeType )
                           .build();

        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return e.getResponse();
        }
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
        try
        {
            final DeployPoint deploy = groupContentManager.store( name, path, request.getInputStream() );

            if ( deploy == null )
            {
                return Response.status( Status.NOT_FOUND )
                               .build();
            }

            return Response.created( uriInfo.getAbsolutePathBuilder()
                                            .path( deploy.getName() )
                                            .path( path )
                                            .build() )
                           .build();
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            return e.getResponse();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to open stream from request: %s", e, e.getMessage() );
            return Response.serverError()
                           .build();
        }
    }
}
