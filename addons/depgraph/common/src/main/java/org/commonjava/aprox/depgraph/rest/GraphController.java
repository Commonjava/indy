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
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.traverse.model.BuildOrder;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.CartoRequestException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.result.*;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.maven.cartographer.request.PathsRequest;
import org.commonjava.maven.cartographer.request.ProjectGraphRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private GraphOps ops;

    @Inject
    private RecipeHelper configHelper;

    public ProjectListResult reindex( final ProjectGraphRequest recipe )
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
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public ProjectPathsResult getPaths( final InputStream configStream )
        throws AproxWorkflowException
    {
        final PathsRequest dto = configHelper.readRecipe( configStream, PathsRequest.class );
        return getPaths( dto );
    }

    public ProjectPathsResult getPaths( final String json )
        throws AproxWorkflowException
    {
        final PathsRequest dto = configHelper.readRecipe( json, PathsRequest.class );
        return getPaths( dto );
    }

    public ProjectPathsResult getPaths( final PathsRequest recipe )
        throws AproxWorkflowException
    {
        configHelper.setRecipeDefaults( recipe );

        try
        {
            return ops.getPaths( recipe );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to discover paths for request: %s. Reason: %s", e, recipe,
                                              e.getMessage() );
        }
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public ProjectErrors errors( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( configStream, ProjectGraphRequest.class );
        return errors( dto );
    }

    public ProjectErrors errors( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( json, ProjectGraphRequest.class );
        return errors( dto );
    }

    public ProjectErrors errors( final ProjectGraphRequest recipe )
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
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public ProjectListResult incomplete( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( configStream, ProjectGraphRequest.class );
        return incomplete( dto );
    }

    public ProjectListResult incomplete( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( json, ProjectGraphRequest.class );
        return incomplete( dto );
    }

    public ProjectListResult incomplete( final ProjectGraphRequest recipe )
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
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public ProjectListResult variable( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( configStream, ProjectGraphRequest.class );
        return variable( dto );
    }

    public ProjectListResult variable( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( json, ProjectGraphRequest.class );
        return variable( dto );
    }

    public ProjectListResult variable( final ProjectGraphRequest recipe )
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
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public MappedProjectsResult ancestryOf( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( configStream, ProjectGraphRequest.class );
        return ancestryOf( dto );
    }

    public MappedProjectsResult ancestryOf( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( json, ProjectGraphRequest.class );
        return ancestryOf( dto );
    }

    public MappedProjectsResult ancestryOf( final ProjectGraphRequest recipe )
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
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public BuildOrder buildOrder( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( configStream, ProjectGraphRequest.class );
        return buildOrder( dto );
    }

    public BuildOrder buildOrder( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( json, ProjectGraphRequest.class );
        return buildOrder( dto );
    }

    public BuildOrder buildOrder( final ProjectGraphRequest recipe )
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
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

    public GraphExport projectGraph( final InputStream configStream )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( configStream, ProjectGraphRequest.class );
        return projectGraph( dto );
    }

    public GraphExport projectGraph( final String json )
        throws AproxWorkflowException
    {
        final ProjectGraphRequest dto = configHelper.readRecipe( json, ProjectGraphRequest.class );
        return projectGraph( dto );
    }

    public GraphExport projectGraph( final ProjectGraphRequest recipe )
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
        catch ( CartoRequestException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Invalid request: %s. Reason: %s", e,
                                              recipe, e.getMessage() );
        }
    }

}
