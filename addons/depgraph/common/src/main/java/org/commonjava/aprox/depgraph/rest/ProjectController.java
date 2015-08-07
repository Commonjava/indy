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
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRecipe;
import org.commonjava.maven.cartographer.recipe.ProjectGraphRelationshipsRecipe;

public class ProjectController
{
    @Inject
    private GraphOps ops;

    @Inject
    private RecipeHelper configHelper;

    public List<ProjectVersionRef> list( final InputStream stream )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe recipe = configHelper.readRecipe( stream, ProjectGraphRecipe.class );
        return list( recipe );
    }

    public List<ProjectVersionRef> list( final ProjectGraphRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.listProjects( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup project listing matching recipe: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public Map<ProjectVersionRef, ProjectVersionRef> parentOf( final InputStream stream )
        throws AproxWorkflowException
    {
        final ProjectGraphRecipe recipe = configHelper.readRecipe( stream, ProjectGraphRecipe.class );
        return parentOf( recipe );
    }

    public Map<ProjectVersionRef, ProjectVersionRef> parentOf( final ProjectGraphRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.getProjectParent( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to lookup parent(s) for recipe: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
    }

    public Map<ProjectVersionRef, Set<ProjectRelationship<?>>> relationshipsDeclaredBy( final InputStream stream )
        throws AproxWorkflowException
    {
        final ProjectGraphRelationshipsRecipe recipe =
            configHelper.readRecipe( stream, ProjectGraphRelationshipsRecipe.class );

        return relationshipsDeclaredBy( recipe );
    }

    public Map<ProjectVersionRef, Set<ProjectRelationship<?>>> relationshipsDeclaredBy( final ProjectGraphRelationshipsRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.getDirectRelationshipsFrom( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to lookup relationships declared by GAVs given in recipe: {}. Reason: {}",
                                              e, recipe, e.getMessage() );
        }
    }

    public Map<ProjectVersionRef, Set<ProjectRelationship<?>>> relationshipsTargeting( final InputStream stream )
        throws AproxWorkflowException
    {
        final ProjectGraphRelationshipsRecipe recipe =
            configHelper.readRecipe( stream, ProjectGraphRelationshipsRecipe.class );

        return relationshipsDeclaredBy( recipe );
    }

    public Map<ProjectVersionRef, Set<ProjectRelationship<?>>> relationshipsTargeting( final ProjectGraphRelationshipsRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.getDirectRelationshipsTo( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to lookup relationships targeting GAVs given in recipe: {}. Reason: {}",
                                              e, recipe, e.getMessage() );
        }
    }

}
