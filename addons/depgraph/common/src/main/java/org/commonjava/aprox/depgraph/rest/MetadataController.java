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

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.util.RecipeHelper;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.MetadataCollation;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.cartographer.recipe.MetadataCollationRecipe;
import org.commonjava.maven.cartographer.recipe.MetadataExtractionRecipe;
import org.commonjava.maven.cartographer.recipe.MetadataUpdateRecipe;

public class MetadataController
{

    @Inject
    private MetadataOps ops;

    @Inject
    private RecipeHelper configHelper;

    public List<ProjectVersionRef> batchUpdate( final InputStream stream )
        throws AproxWorkflowException
    {
        final MetadataUpdateRecipe recipe = configHelper.readRecipe( stream, MetadataUpdateRecipe.class );
        return batchUpdate( recipe );
    }

    public List<ProjectVersionRef> batchUpdate( final MetadataUpdateRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.updateMetadata( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to update metadata for recipe: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
    }

    public Map<ProjectVersionRef, Map<String, String>> getMetadata( final InputStream stream )
        throws AproxWorkflowException
    {
        final MetadataExtractionRecipe recipe = configHelper.readRecipe( stream, MetadataExtractionRecipe.class );
        return getMetadata( recipe );
    }

    public Map<ProjectVersionRef, Map<String, String>> getMetadata( final MetadataExtractionRecipe recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.getMetadata( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve metadata for recipe: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
    }

    public MetadataCollation getCollation( final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        final MetadataCollationRecipe dto = configHelper.readRecipe( configStream, MetadataCollationRecipe.class );
        return getCollation( dto );
    }

    public MetadataCollation getCollation( final String json )
        throws AproxWorkflowException
    {
        final MetadataCollationRecipe dto = configHelper.readRecipe( json, MetadataCollationRecipe.class );
        return getCollation( dto );
    }

    public MetadataCollation getCollation( final MetadataCollationRecipe dto )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( dto );
        try
        {
            return ops.collate( dto );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to resolve or collate graph contents by metadata: {}. Reason: {}",
                                              e, dto, e.getMessage() );
        }
    }

}
