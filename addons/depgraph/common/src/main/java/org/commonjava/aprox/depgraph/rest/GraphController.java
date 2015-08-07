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
package org.commonjava.aprox.depgraph.rest;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.util.RecipeHelper;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.GraphExport;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.maven.cartographer.recipe.PathsRecipe;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphOps ops;

    @Inject
    private RecipeHelper configHelper;

    public List<ProjectVersionRef> reindex( final ProjectGraphRecipe recipe )
        throws AproxWorkflowException
    {
        try
        {
            return ops.reindex( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to reindex: {}. Reason: {}", e, recipe, e.getMessage() );
        }
    }

    public Map<ProjectVersionRef, List<List<ProjectRelationship<?>>>> getPaths( final InputStream configStream )
        throws AproxWorkflowException
    {
        final PathsRecipe dto = configHelper.readRecipe( configStream, PathsRecipe.class );
        return getPaths( dto );
    }

    public Map<ProjectVersionRef, List<List<ProjectRelationship<?>>>> getPaths( final String json )
        throws AproxWorkflowException
    {
        final PathsRecipe dto = configHelper.readRecipe( json, PathsRecipe.class );
        return getPaths( dto );
    }

    public Map<ProjectVersionRef, List<List<ProjectRelationship<?>>>> getPaths( final PathsRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.getPaths( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to discover paths for recipe: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
    }

    public Map<ProjectVersionRef, String> errors( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( configStream, ProjectGraphRecipe.class );
        return errors( dto );
    }

    public Map<ProjectVersionRef, String> errors( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( json, ProjectGraphRecipe.class );
        return errors( dto );
    }

    public Map<ProjectVersionRef, String> errors( final ProjectGraphRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            logger.debug( "Retrieving project errors: {}", recipe );
            return ops.getProjectErrors( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup resolution errors for: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
    }

    public Set<ProjectVersionRef> incomplete( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( configStream, ProjectGraphRecipe.class );
        return incomplete( dto );
    }

    public Set<ProjectVersionRef> incomplete( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( json, ProjectGraphRecipe.class );
        return incomplete( dto );
    }

    public Set<ProjectVersionRef> incomplete( final ProjectGraphRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.getIncomplete( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup incomplete subgraphs for: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
    }

    public Set<ProjectVersionRef> variable( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( configStream, ProjectGraphRecipe.class );
        return variable( dto );
    }

    public Set<ProjectVersionRef> variable( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( json, ProjectGraphRecipe.class );
        return variable( dto );
    }

    public Set<ProjectVersionRef> variable( final ProjectGraphRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.getVariable( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup variable subgraphs for: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
    }

    public Map<ProjectVersionRef, List<ProjectVersionRef>> ancestryOf( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( configStream, ProjectGraphRecipe.class );
        return ancestryOf( dto );
    }

    public Map<ProjectVersionRef, List<ProjectVersionRef>> ancestryOf( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( json, ProjectGraphRecipe.class );
        return ancestryOf( dto );
    }

    public Map<ProjectVersionRef, List<ProjectVersionRef>> ancestryOf( final ProjectGraphRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.getAncestry( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup ancestry for: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
    }

    public BuildOrder buildOrder( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( configStream, ProjectGraphRecipe.class );
        return buildOrder( dto );
    }

    public BuildOrder buildOrder( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( json, ProjectGraphRecipe.class );
        return buildOrder( dto );
    }

    public BuildOrder buildOrder( final ProjectGraphRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.getBuildOrder( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup build order for: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
    }

    public GraphExport projectGraph( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( configStream, ProjectGraphRecipe.class );
        return projectGraph( dto );
    }

    public GraphExport projectGraph( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe dto = configHelper.readRecipe( json, ProjectGraphRecipe.class );
        return projectGraph( dto );
    }

    public GraphExport projectGraph( final ProjectGraphRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.exportGraph( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to export project graph for: {}. Reason: {}", e, recipe,
                                              e.getMessage() );
        }
    }

}
