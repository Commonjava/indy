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
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.result.MetadataCollationResult;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.cartographer.request.MetadataCollationRequest;
import org.commonjava.maven.cartographer.request.MetadataExtractionRequest;
import org.commonjava.maven.cartographer.request.MetadataUpdateRequest;
import org.commonjava.maven.cartographer.result.MetadataResult;
import org.commonjava.maven.cartographer.result.ProjectListResult;

public class MetadataController
{

    @Inject
    private MetadataOps ops;

    @Inject
    private RecipeHelper configHelper;

    public ProjectListResult batchUpdate( final InputStream stream )
        throws AproxWorkflowException
    {
        final MetadataUpdateRequest recipe = configHelper.readRecipe( stream, MetadataUpdateRequest.class );
        return batchUpdate( recipe );
    }

    public ProjectListResult batchUpdate( final MetadataUpdateRequest recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.updateMetadata( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to update metadata for request: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public MetadataResult getMetadata( final InputStream stream )
        throws AproxWorkflowException
    {
        final MetadataExtractionRequest recipe = configHelper.readRecipe( stream, MetadataExtractionRequest.class );
        return getMetadata( recipe );
    }

    public MetadataResult getMetadata( final MetadataExtractionRequest recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.getMetadata( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve metadata for request: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public MetadataCollationResult getCollation( final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        final MetadataCollationRequest dto = configHelper.readRecipe( configStream, MetadataCollationRequest.class );
        return getCollation( dto );
    }

    public MetadataCollationResult getCollation( final String json )
        throws AproxWorkflowException
    {
        final MetadataCollationRequest dto = configHelper.readRecipe( json, MetadataCollationRequest.class );
        return getCollation( dto );
    }

    public MetadataCollationResult getCollation( final MetadataCollationRequest recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.collate( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to resolve or collate graph contents by metadata: {}. Reason: {}",
                                              e, recipe, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

}
