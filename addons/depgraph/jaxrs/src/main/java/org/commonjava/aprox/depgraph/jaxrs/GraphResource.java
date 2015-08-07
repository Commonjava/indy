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

import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.formatOkResponseWithJsonEntity;
import static org.commonjava.aprox.bind.jaxrs.util.ResponseUtils.throwError;
import static org.commonjava.aprox.util.ApplicationContent.application_json;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.jaxrs.AproxResources;
import org.commonjava.aprox.bind.jaxrs.util.JaxRsUriFormatter;
import org.commonjava.aprox.depgraph.rest.GraphController;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.dto.GraphExport;
import org.commonjava.maven.cartographer.recipe.PathsRecipe;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path( "/api/depgraph/graph" )
@Consumes( { "application/json", "application/aprox*+json" } )
@Produces( { "applicaiton/json", "application/aprox*+json" } )
public class GraphResource
    implements AproxResources
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphController controller;

    @Path( "/paths" )
    @POST
    public Map<ProjectVersionRef, List<List<ProjectRelationship<?>>>> getPaths( final PathsRecipe recipe )
    {
        try
        {
            return controller.getPaths( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @Path( "/errors" )
    @POST
    public Map<ProjectVersionRef, String> errors( final ProjectGraphRecipe recipe )
    {
        try
        {
            return controller.errors( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }

        return null;
    }

    @Path( "/reindex" )
    @POST
    public List<ProjectVersionRef> reindex( final ProjectGraphRecipe recipe )
    {
        try
        {
            return controller.reindex( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @Path( "/incomplete" )
    @POST
    public Set<ProjectVersionRef> incomplete( final ProjectGraphRecipe recipe )
    {
        try
        {
            return controller.incomplete( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @Path( "/variable" )
    @POST
    public Set<ProjectVersionRef> variable( final ProjectGraphRecipe recipe )
    {
        try
        {
            return controller.variable( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            logger.error( e.getMessage(), e );
            throwError( e );
        }
        return null;
    }

    @Path( "/ancestry" )
    @POST
    public Map<ProjectVersionRef, List<ProjectVersionRef>> ancestryOf( final ProjectGraphRecipe recipe )
    {
        try
        {
            return controller.ancestryOf( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }

    @Path( "/build-order" )
    @POST
    public BuildOrder buildOrder( final ProjectGraphRecipe recipe )
    {
        try
        {
            return controller.buildOrder( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }

    @Path( "/export" )
    @POST
    public GraphExport graph( final ProjectGraphRecipe recipe )
    {
        try
        {
            return controller.projectGraph( recipe );
        }
        catch ( final AproxWorkflowException e )
        {
            throwError( e );
        }
        return null;
    }

}
