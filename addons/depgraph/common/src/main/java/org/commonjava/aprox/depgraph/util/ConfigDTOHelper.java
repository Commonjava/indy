/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.util;

import static org.apache.commons.io.IOUtils.readLines;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.dto.MetadataCollationDTO;
import org.commonjava.aprox.depgraph.dto.WebOperationConfigDTO;
import org.commonjava.aprox.depgraph.json.JsonUtils;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.agg.AggregatorConfig;
import org.commonjava.maven.cartographer.dto.GraphComposition;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class ConfigDTOHelper
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private PresetSelector presets;

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private LocationExpander locationExpander;

    public WebOperationConfigDTO readWebOperationDTO( final InputStream configStream )
        throws AproxWorkflowException
    {
        try
        {
            return readWebOperationDTO( IOUtils.toString( configStream ) );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read configuration JSON from request body. Reason: %s", e, e.getMessage() );
        }
    }

    public WebOperationConfigDTO readWebOperationDTO( final String json )
        throws AproxWorkflowException
    {
        logger.info( "Got configuration JSON:\n\n%s\n\n", json );
        final WebOperationConfigDTO dto = serializer.fromString( json, WebOperationConfigDTO.class );
        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "No configuration found in request body!" );
        }

        try
        {
            dto.calculateLocations( locationExpander );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "One or more sources/excluded sources is invalid: %s", e, e.getMessage() );
        }

        return dto;
    }

    public GraphComposition readGraphComposition( final String json )
        throws AproxWorkflowException
    {
        final GraphComposition dto = serializer.fromString( json, GraphComposition.class );

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
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Cannot read GraphComposition JSON from stream: %s", e, e.getMessage() );
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
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Cannot read MetadataCollationDTO JSON from stream: %s", e,
                                              e.getMessage() );
        }
    }

    public MetadataCollationDTO readCollationDTO( final String json )
        throws AproxWorkflowException
    {
        final MetadataCollationDTO dto = serializer.fromString( json, MetadataCollationDTO.class );
        if ( dto == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "No collation configuration found in request body!" );
        }

        dto.resolveFilters( presets, config.getDefaultWebFilterPreset() );

        try
        {
            dto.calculateLocations( locationExpander );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "One or more sources/excluded sources is invalid: %s", e, e.getMessage() );
        }

        return dto;
    }

    public AggregatorConfig readAggregatorConfig( final InputStream stream )
        throws AproxWorkflowException
    {
        List<String> lines;
        try
        {
            lines = readLines( stream );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Failed to read GAV root listing: %s", e, e.getMessage() );
        }

        return readAggregatorConfig( lines );
    }

    public AggregatorConfig readAggregatorConfig( final String listing )
        throws AproxWorkflowException
    {
        final List<String> lines = Arrays.asList( listing.split( "\r?\n" ) );
        return readAggregatorConfig( lines );
    }

    public AggregatorConfig readAggregatorConfig( final List<String> lines )
        throws AproxWorkflowException
    {
        final Set<ProjectVersionRef> refs = new HashSet<ProjectVersionRef>();

        for ( final String line : lines )
        {
            if ( line.trim()
                     .length() < 1 || line.trim()
                                          .startsWith( "#" ) )
            {
                continue;
            }

            final ProjectVersionRef ref = JsonUtils.parseProjectVersionRef( line );
            if ( ref != null )
            {
                refs.add( ref );
            }
        }

        return new AggregatorConfig( refs );
    }

}
