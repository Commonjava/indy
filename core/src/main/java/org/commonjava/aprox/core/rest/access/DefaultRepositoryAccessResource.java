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
import javax.ws.rs.core.Response;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.rest.AproxWorkflowException;

@Path( "/repository" )
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
}
