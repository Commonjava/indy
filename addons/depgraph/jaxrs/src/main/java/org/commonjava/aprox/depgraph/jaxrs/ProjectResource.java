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
package org.commonjava.aprox.depgraph.jaxrs;

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.throwError;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.depgraph.rest.ProjectController;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRecipe;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRelationshipsRecipe;

@Path( "/api/depgraph/project" )
@Consumes( { "application/json", "application/aprox*+json" } )
@Produces( { "applicaiton/json", "application/aprox*+json" } )
public class ProjectResource
    implements AproxResources
{
    @Inject
    private ProjectController controller;

    @Path( "/list" )
    @POST
    public List<ProjectVersionRef> list( final ProjectGraphRecipe recipe )
    {
        try
        {
            return controller.list( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }

        // not used; throwError will supercede with WebApplicationException
        return null;
    }

    @Path( "/parent" )
    @POST
    public Map<ProjectVersionRef, ProjectVersionRef> parentOf( final ProjectGraphRecipe recipe )
    {
        try
        {
            return controller.parentOf( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }

        return null;
    }

    @Path( "/relationships" )
    @POST
    public Map<ProjectVersionRef, Set<ProjectRelationship<?>>> dependenciesOf( final ProjectGraphRelationshipsRecipe recipe )
    {
        try
        {
            return controller.relationshipsDeclaredBy( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }

        return null;
    }

    @Path( "/targeting" )
    @POST
    public Map<ProjectVersionRef, Set<ProjectRelationship<?>>> relationshipsTargeting( final ProjectGraphRelationshipsRecipe recipe )
    {
        try
        {
            return controller.relationshipsTargeting( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }

        return null;
    }

}
