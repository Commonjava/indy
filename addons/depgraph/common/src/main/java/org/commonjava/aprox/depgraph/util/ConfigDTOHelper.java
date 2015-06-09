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
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.dto.MetadataCollationDTO;
import org.commonjava.aprox.depgraph.dto.PathsDTO;
import org.commonjava.aprox.depgraph.dto.WebPomDTO;
import org.commonjava.aprox.depgraph.dto.WebOperationConfigDTO;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ConfigDTOHelper
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ObjectMapper serializer;

    @Inject
    private PresetSelector presets;

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private LocationExpander locationExpander;

    @Inject
    private StoreDataManager dataManager;

    public WebOperationConfigDTO readWebOperationDTO( final InputStream configStream )
        throws AproxWorkflowException
    {
        try
        {
            return readWebOperationDTO( IOUtils.toString( configStream ) );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read configuration JSON from request body. Reason: {}", e, e.getMessage() );
        }
    }

    public WebOperationConfigDTO readWebOperationDTO( final String json )
        throws AproxWorkflowException
    {
        logger.info( "Got configuration JSON:\n\n{}\n\n", json );
        WebOperationConfigDTO dto;
        try
        {
            dto = serializer.readValue( json, WebOperationConfigDTO.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize DTO from JSON. Reason: %s", e, e.getMessage() );
        }

        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "No configuration found in request body!" );
        }

        try
        {
            dto.calculateLocations( locationExpander, dataManager );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "One or more sources/excluded sources is invalid: {}", e, e.getMessage() );
        }

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
            throw new AproxWorkflowException( "Failed to read configuration JSON from request body. Reason: {}",
                                              e, e.getMessage() );
        }
    }

    public PathsDTO readPathsDTO( final String json )
        throws AproxWorkflowException
    {
        logger.info( "Got paths configuration JSON:\n\n{}\n\n", json );
        PathsDTO dto;
        try
        {
            dto = serializer.readValue( json, PathsDTO.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize PathsDTO from JSON. Reason: %s", e, e.getMessage() );
        }

        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "No PathsDTO found in request body!" );
        }

        try
        {
            dto.calculateLocations( locationExpander, dataManager );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "One or more sources/excluded sources is invalid: {}", e, e.getMessage() );
        }

        return dto;
    }

    public GraphComposition readGraphComposition( final String json )
        throws AproxWorkflowException
    {
        GraphComposition dto;
        try
        {
            dto = serializer.readValue( json, GraphComposition.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize DTO from JSON. Reason: %s", e, e.getMessage() );
        }

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

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

    public MetadataCollationDTO readCollationDTO( final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        try
        {
            return readCollationDTO( IOUtils.toString( configStream ) );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Cannot read MetadataCollationDTO JSON from stream: {}", e,
                                              e.getMessage() );
        }
    }

    public MetadataCollationDTO readCollationDTO( final String json )
        throws AproxWorkflowException
    {
        MetadataCollationDTO dto;
        try
        {
            dto = serializer.readValue( json, MetadataCollationDTO.class );
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

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        try
        {
            dto.calculateLocations( locationExpander, dataManager );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "One or more sources/excluded sources is invalid: {}", e, e.getMessage() );
        }

        return dto;
    }

    public WebPomDTO readPomDTO( final InputStream stream )
        throws AproxWorkflowException
    {
        try
        {
            final String json = IOUtils.toString( stream );
            return readPomDTO( json );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Cannot read WebPomDTO JSON from stream: {}", e, e.getMessage() );
        }
    }

    public WebPomDTO readPomDTO( final String json )
        throws AproxWorkflowException
    {
        WebPomDTO dto;
        try
        {
            dto = serializer.readValue( json, WebPomDTO.class );
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

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        try
        {
            dto.calculateLocations( locationExpander, dataManager );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "AProx source store %s is invalid: %s", e,
                                              dto.getSource(), e.getMessage() );
        }

        return dto;
    }

}
