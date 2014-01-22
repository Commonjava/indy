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
package org.commonjava.aprox.depgraph.rest;

import static org.apache.commons.lang.StringUtils.join;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.dto.MetadataCollationDTO;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.MetadataCollation;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.cartographer.preset.PresetSelector;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;

import com.google.gson.reflect.TypeToken;

@RequestScoped
public class MetadataController
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private MetadataOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private LocationExpander locationExpander;

    @Inject
    private PresetSelector presets;

    @Inject
    private AproxDepgraphConfig config;

    public void batchUpdate( final InputStream stream, final String encoding )
        throws AproxWorkflowException
    {
        String enc = encoding;
        if ( enc == null )
        {
            enc = "UTF-8";
        }

        final TypeToken<Map<ProjectVersionRef, Map<String, String>>> tt = new TypeToken<Map<ProjectVersionRef, Map<String, String>>>()
        {
        };

        final Map<ProjectVersionRef, Map<String, String>> batch = serializer.fromStream( stream, enc, tt );
        if ( batch == null || batch.isEmpty() )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_MODIFIED, "No changes found in metadata request." );
        }

        for ( final Map.Entry<ProjectVersionRef, Map<String, String>> entry : batch.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final Map<String, String> metadata = entry.getValue();

            logger.info( "Adding metadata for: %s\n\n  ", ref, join( metadata.entrySet(), "\n  " ) );
            ops.updateMetadata( ref, metadata );
        }
    }

    public String getMetadata( final String groupId, final String artifactId, final String version )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        Map<String, String> metadata = null;
        try
        {
            metadata = ops.getMetadata( ref );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve metadata map for: %s. Reason: %s", e, ref, e.getMessage() );
        }

        return metadata == null ? null : serializer.toString( metadata );
    }

    public String getMetadataValue( final String groupId, final String artifactId, final String version, final String key )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final String value = ops.getMetadataValue( ref, key );
            return value == null ? null : serializer.toString( Collections.singletonMap( key, value ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve metadata map for: %s. Reason: %s", e, ref, e.getMessage() );
        }
    }

    public void updateMetadata( final String groupId, final String artifactId, final String version, final InputStream stream, final String encoding )
        throws AproxWorkflowException
    {
        String enc = encoding;
        if ( enc == null )
        {
            enc = "UTF-8";
        }

        final TypeToken<Map<String, String>> tt = new TypeToken<Map<String, String>>()
        {
        };

        final Map<String, String> metadata = serializer.fromStream( stream, enc, tt );

        if ( metadata == null || metadata.isEmpty() )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "No metadata updates found in request body!" );
        }

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

        logger.info( "Adding metadata for: %s\n\n  ", ref, join( metadata.entrySet(), "\n  " ) );

        ops.updateMetadata( ref, metadata );
    }

    public String getCollation( final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        final MetadataCollationDTO dto = readCollationDTO( configStream, encoding );

        try
        {
            final MetadataCollation result = ops.collate( dto );
            return result == null ? null : serializer.toString( result );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to resolve or collate graph contents by metadata: %s. Reason: %s", e, dto, e.getMessage() );
        }
    }

    private MetadataCollationDTO readCollationDTO( final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        String enc = encoding;
        if ( enc == null )
        {
            enc = "UTF-8";
        }

        final MetadataCollationDTO dto = serializer.fromStream( configStream, enc, MetadataCollationDTO.class );
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

}
