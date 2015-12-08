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
package org.commonjava.indy.depgraph.jaxrs.resolve;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.throwError;
import static org.commonjava.indy.util.ApplicationContent.application_indy_star_json;
import static org.commonjava.indy.util.ApplicationContent.application_json;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.depgraph.rest.ResolverController;
import org.commonjava.cartographer.request.MultiGraphRequest;

@Path( "/api/depgraph/graph" )
@Consumes( { application_json, application_indy_star_json } )
public class ResolverResource
    implements IndyResources
{

    @Inject
    private ResolverController controller;

    @POST
    public Response resolveGraph( final MultiGraphRequest recipe )
    {
        try
        {
            controller.resolve( recipe );
            return Response.ok()
                           .build();

        }
        catch ( final IndyWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }

}
