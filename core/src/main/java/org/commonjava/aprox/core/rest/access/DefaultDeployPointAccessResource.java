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

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.access.DeployPointAccessResource;
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
    private StoreDataManager proxyManager;

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
    public Response createContent( @PathParam( "name" ) final String name, @PathParam( "path" ) final String path,
                                   @Context final HttpServletRequest request )
    {
        Response response = null;
        DeployPoint deploy = null;
        try
        {
            deploy = getArtifactStore( name );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( "Failed to retrieve artifact store: %s. Reason: %s", e, name, e.getMessage() );
            response = e.getResponse();
        }

        if ( response == null )
        {
            try
            {
                getFileManager().store( deploy, path, request.getInputStream() );

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
            catch ( final AproxWorkflowException e )
            {
                logger.error( "Failed to upload: %s to: %s. Reason: %s", e, path, name, e.getMessage() );
                response = e.getResponse();
            }
        }

        return response;
    }

    @Override
    protected DeployPoint getArtifactStore( final String name )
        throws AproxWorkflowException
    {
        try
        {
            return proxyManager.getDeployPoint( name );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( Response.serverError()
                                                     .build(), "Failed to retrieve deploy store: %s. Reason: %s", e,
                                             name, e.getMessage() );
        }
    }

}
