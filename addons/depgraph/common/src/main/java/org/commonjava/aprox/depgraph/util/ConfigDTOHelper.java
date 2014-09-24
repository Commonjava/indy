/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
import org.commonjava.aprox.depgraph.dto.WebBomDTO;
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
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "No configuration found in request body!" );
        }

        try
        {
            dto.calculateLocations( locationExpander, dataManager );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "One or more sources/excluded sources is invalid: {}", e, e.getMessage() );
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
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Cannot read GraphComposition JSON from stream: {}", e, e.getMessage() );
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
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Cannot read MetadataCollationDTO JSON from stream: {}", e,
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
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "No collation configuration found in request body!" );
        }

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        try
        {
            dto.calculateLocations( locationExpander, dataManager );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "One or more sources/excluded sources is invalid: {}", e, e.getMessage() );
        }

        return dto;
    }

    public WebBomDTO readBomDTO( final InputStream stream )
        throws AproxWorkflowException
    {
        try
        {
            final String json = IOUtils.toString( stream );
            return readBomDTO( json );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST,
                                              "Cannot read WebBomDTO JSON from stream: {}", e, e.getMessage() );
        }
    }

    public WebBomDTO readBomDTO( final String json )
        throws AproxWorkflowException
    {
        WebBomDTO dto;
        try
        {
            dto = serializer.readValue( json, WebBomDTO.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize DTO from JSON. Reason: %s", e, e.getMessage() );
        }

        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST,
                                              "No BOM configuration found in request body!" );
        }

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        try
        {
            dto.calculateLocations( locationExpander, dataManager );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "AProx source store %s is invalid: %s", e,
                                              dto.getSource(), e.getMessage() );
        }

        return dto;
    }

}
