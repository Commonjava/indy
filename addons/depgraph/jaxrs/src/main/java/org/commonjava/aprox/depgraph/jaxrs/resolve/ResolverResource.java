/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.depgraph.jaxrs.resolve;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.throwError;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.ResolverController;
import org.commonjava.maven.cartographer.recipe.MultiGraphResolverRecipe;

@Path( "/api/depgraph/resolve" )
@Consumes( { "application/json", "application/aprox*+json" } )
@Produces( { "applicaiton/json", "application/aprox*+json" } )
public class ResolverResource
    implements AproxResources
{

    @Inject
    private ResolverController controller;

    @POST
    public Response resolveGraph( final MultiGraphResolverRecipe recipe )
    {
        try
        {
            controller.resolve( recipe );
            return Response.ok()
                           .build();

        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }

}
