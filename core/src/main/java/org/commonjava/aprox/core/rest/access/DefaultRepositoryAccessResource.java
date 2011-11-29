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
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.rest.access.RepositoryAccessResource;
import org.commonjava.util.logging.Logger;

@Path( "/repository" )
@RequestScoped
public class DefaultRepositoryAccessResource
    extends AbstractSimpleAccessResource<Repository>
    implements RepositoryAccessResource
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyManager;

    // @Context
    // private UriInfo uriInfo;

    @Override
    protected Repository getArtifactStore( final String name )
    {
        try
        {
            return proxyManager.getRepository( name );
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to retrieve proxy: %s. Reason: %s", e, name, e.getMessage() );
            throw new WebApplicationException(
                                               Response.status( Status.INTERNAL_SERVER_ERROR ).build() );
        }
    }
}
