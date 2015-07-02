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
package org.commonjava.aprox.depgraph.util;

import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.dto.DownlogDTO;
import org.commonjava.aprox.depgraph.dto.PathsDTO;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.dto.MetadataCollationRecipe;
import org.commonjava.maven.cartographer.dto.PomRecipe;
import org.commonjava.maven.cartographer.dto.RepositoryContentRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class RecipeHelper
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper mapper;

    @Inject
    private AproxDepgraphConfig config;

    protected RecipeHelper()
    {
    }

    public RecipeHelper( final AproxDepgraphConfig config, final ObjectMapper mapper )
    {
        this.config = config;
        this.mapper = mapper;
    }

    public RepositoryContentRecipe readRepositoryContentRecipe( final InputStream configStream )
        throws AproxWorkflowException
    {
        try
        {
            return readRepositoryContentRecipe( IOUtils.toString( configStream ) );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to read RepositoryContentRecipe JSON from request body. Reason: {}",
                                              e, e.getMessage() );
        }
    }

    public RepositoryContentRecipe readRepositoryContentRecipe( final String json )
        throws AproxWorkflowException
    {
        logger.info( "Got configuration JSON:\n\n{}\n\n", json );
        RepositoryContentRecipe dto;
        try
        {
            dto = mapper.readValue( json, RepositoryContentRecipe.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize RepositoryContentRecipe from JSON. Reason: %s", e,
                                              e.getMessage() );
        }

        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "No configuration found in request body!" );
        }

        dto.setDefaultPreset( config.getDefaultWebFilterPreset() );

        return dto;
    }

    public DownlogDTO readDownlogDTO( final InputStream configStream )
        throws AproxWorkflowException
    {
        try
        {
            return readDownlogDTO( IOUtils.toString( configStream ) );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read DownlogDTO JSON from request body. Reason: {}", e,
                                              e.getMessage() );
        }
    }

    public DownlogDTO readDownlogDTO( final String json )
        throws AproxWorkflowException
    {
        logger.info( "Got configuration JSON:\n\n{}\n\n", json );
        DownlogDTO dto;
        try
        {
            dto = mapper.readValue( json, DownlogDTO.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize DownlogDTO from JSON. Reason: %s", e,
                                              e.getMessage() );
        }

        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "No configuration found in request body!" );
        }

        dto.setDefaultPreset( config.getDefaultWebFilterPreset() );

        return dto;
    }

    public PathsDTO readPathsDTO( final InputStream configStream )
        throws AproxWorkflowException
    {
        try
        {
            return readPathsDTO( IOUtils.toString( configStream ) );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read configuration JSON from request body. Reason: {}", e,
                                              e.getMessage() );
        }
    }

    public PathsDTO readPathsDTO( final String json )
        throws AproxWorkflowException
    {
        logger.info( "Got paths configuration JSON:\n\n{}\n\n", json );
        PathsDTO dto;
        try
        {
            dto = mapper.readValue( json, PathsDTO.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize PathsDTO from JSON. Reason: %s", e, e.getMessage() );
        }

        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "No PathsDTO found in request body!" );
        }

        dto.setDefaultPreset( config.getDefaultWebFilterPreset() );

        return dto;
    }

    public GraphComposition readGraphComposition( final String json )
        throws AproxWorkflowException
    {
        GraphComposition dto;
        try
        {
            dto = mapper.readValue( json, GraphComposition.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize GraphComposition from JSON. Reason: %s", e,
                                              e.getMessage() );
        }

        dto.setDefaultPreset( config.getDefaultWebFilterPreset() );

        return dto;
    }

    public GraphComposition readGraphComposition( final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        try
        {
            return readGraphComposition( IOUtils.toString( configStream ) );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Cannot read GraphComposition JSON from stream: {}", e, e.getMessage() );
        }

    }

    public MetadataCollationRecipe readCollationRecipe( final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        try
        {
            return readCollationRecipe( IOUtils.toString( configStream ) );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Cannot read MetadataCollationRecipe JSON from stream: {}", e,
                                              e.getMessage() );
        }
    }

    public MetadataCollationRecipe readCollationRecipe( final String json )
        throws AproxWorkflowException
    {
        MetadataCollationRecipe dto;
        try
        {
            dto = mapper.readValue( json, MetadataCollationRecipe.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize DTO from JSON. Reason: %s", e, e.getMessage() );
        }

        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "No collation configuration found in request body!" );
        }

        dto.setDefaultPreset( config.getDefaultWebFilterPreset() );

        return dto;
    }

    public PomRecipe readPomRecipe( final InputStream stream )
        throws AproxWorkflowException
    {
        try
        {
            final String json = IOUtils.toString( stream );
            return readPomRecipe( json );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Cannot read PomRecipe JSON from stream: {}", e, e.getMessage() );
        }
    }

    public PomRecipe readPomRecipe( final String json )
        throws AproxWorkflowException
    {
        PomRecipe dto;
        try
        {
            dto = mapper.readValue( json, PomRecipe.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize DTO from JSON. Reason: %s", e, e.getMessage() );
        }

        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "No POM configuration found in request body!" );
        }

        dto.setDefaultPreset( config.getDefaultWebFilterPreset() );

        return dto;
    }

}
