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
package org.commonjava.indy.depgraph.rest;

import java.io.InputStream;

import javax.inject.Inject;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.depgraph.util.RecipeHelper;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.request.ProjectGraphRequest;
import org.commonjava.cartographer.result.MetadataCollationResult;
import org.commonjava.cartographer.ops.MetadataOps;
import org.commonjava.cartographer.request.MetadataCollationRequest;
import org.commonjava.cartographer.request.MetadataExtractionRequest;
import org.commonjava.cartographer.request.MetadataUpdateRequest;
import org.commonjava.cartographer.result.MetadataResult;
import org.commonjava.cartographer.result.ProjectListResult;

public class MetadataController
{

    @Inject
    private MetadataOps ops;

    @Inject
    private RecipeHelper configHelper;

    public ProjectListResult batchUpdate( final InputStream stream )
        throws IndyWorkflowException
    {
        final MetadataUpdateRequest recipe = configHelper.readRecipe( stream, MetadataUpdateRequest.class );
        return batchUpdate( recipe );
    }

    public ProjectListResult batchUpdate( final MetadataUpdateRequest recipe )
        throws IndyWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.updateMetadata( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new IndyWorkflowException( "Failed to update metadata for request: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public MetadataResult getMetadata( final InputStream stream )
        throws IndyWorkflowException
    {
        final MetadataExtractionRequest recipe = configHelper.readRecipe( stream, MetadataExtractionRequest.class );
        return getMetadata( recipe );
    }

    public MetadataResult getMetadata( final MetadataExtractionRequest recipe )
        throws IndyWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.getMetadata( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new IndyWorkflowException( "Failed to retrieve metadata for request: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public MetadataCollationResult getCollation( final InputStream configStream, final String encoding )
        throws IndyWorkflowException
    {
        final MetadataCollationRequest dto = configHelper.readRecipe( configStream, MetadataCollationRequest.class );
        return getCollation( dto );
    }

    public MetadataCollationResult getCollation( final String json )
        throws IndyWorkflowException
    {
        final MetadataCollationRequest dto = configHelper.readRecipe( json, MetadataCollationRequest.class );
        return getCollation( dto );
    }

    public MetadataCollationResult getCollation( final MetadataCollationRequest recipe )
        throws IndyWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );
        try
        {
            return ops.collate( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new IndyWorkflowException(
                                              "Failed to graph or collate graph contents by metadata: {}. Reason: {}",
                                              e, recipe, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public ProjectListResult rescan( ProjectGraphRequest recipe )
            throws IndyWorkflowException
    {
        try
        {
            return ops.rescanMetadata( recipe );
        }
        catch ( CartoDataException e )
        {
            throw new IndyWorkflowException( "Failed to rescan graph metadata: {}. Reason: {}", e, recipe, e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }
}
