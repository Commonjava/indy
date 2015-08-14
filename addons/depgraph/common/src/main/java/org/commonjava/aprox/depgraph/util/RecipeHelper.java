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
import org.commonjava.aprox.depgraph.model.DownlogRequest;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.cartographer.request.AbstractGraphRequest;
import org.commonjava.maven.cartographer.request.GraphBasedRequest;
import org.commonjava.maven.cartographer.request.GraphComposition;
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

    public void setRecipeDefaults( final AbstractGraphRequest recipe )
        throws AproxWorkflowException
    {
        if ( recipe == null )
        {
            logger.warn( "Configuration DTO is missing." );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "JSON configuration not supplied" );
        }

        recipe.setDefaultPreset( this.config.getDefaultWebFilterPreset() );
    }

    public DownlogRequest readDownlogDTO( final InputStream configStream )
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

    public DownlogRequest readDownlogDTO( final String json )
        throws AproxWorkflowException
    {
        logger.info( "Got configuration JSON:\n\n{}\n\n", json );
        DownlogRequest dto;
        try
        {
            dto = mapper.readValue( json, DownlogRequest.class );
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

    public <T extends GraphBasedRequest> T readRecipe( final InputStream stream, final Class<T> type )
        throws AproxWorkflowException
    {
        try
        {
            final String json = IOUtils.toString( stream );
            return readRecipe( json, type );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Cannot read graph request JSON from stream: {}", e, e.getMessage() );
        }
    }

    public <T extends GraphBasedRequest> T readRecipe( final String json, final Class<T> type )
        throws AproxWorkflowException
    {
        T dto;
        try
        {
            dto = mapper.readValue( json, type );
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
