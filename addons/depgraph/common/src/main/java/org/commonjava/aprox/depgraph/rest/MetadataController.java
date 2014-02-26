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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.depgraph.dto.MetadataCollationDTO;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.depgraph.util.ConfigDTOHelper;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.MetadataCollation;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

@ApplicationScoped
public class MetadataController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MetadataOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    @Inject
    private ConfigDTOHelper configHelper;

    public void batchUpdate( final InputStream stream, final String encoding )
        throws AproxWorkflowException
    {
        final String json = readJson( stream, encoding );
        batchUpdate( json );
    }

    private String readJson( final InputStream stream, final String encoding )
        throws AproxWorkflowException
    {
        try
        {
            return encoding == null ? IOUtils.toString( stream ) : IOUtils.toString( stream, encoding );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Cannot read metadata mapping JSON from stream: {}", e, e.getMessage() );
        }
    }

    public void batchUpdate( final String json )
        throws AproxWorkflowException
    {
        final TypeToken<Map<ProjectVersionRef, Map<String, String>>> tt = new TypeToken<Map<ProjectVersionRef, Map<String, String>>>()
        {
        };

        final Map<ProjectVersionRef, Map<String, String>> batch = serializer.fromString( json, tt );
        if ( batch == null || batch.isEmpty() )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_MODIFIED, "No changes found in metadata request." );
        }

        for ( final Map.Entry<ProjectVersionRef, Map<String, String>> entry : batch.entrySet() )
        {
            final ProjectVersionRef ref = entry.getKey();
            final Map<String, String> metadata = entry.getValue();

            logger.info( "Adding metadata for: {}\n\n  ", ref, join( metadata.entrySet(), "\n  " ) );
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
            throw new AproxWorkflowException( "Failed to retrieve metadata map for: {}. Reason: {}", e, ref, e.getMessage() );
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
            throw new AproxWorkflowException( "Failed to retrieve metadata map for: {}. Reason: {}", e, ref, e.getMessage() );
        }
    }

    public void updateMetadata( final String groupId, final String artifactId, final String version, final InputStream stream, final String encoding )
        throws AproxWorkflowException
    {
        final String json = readJson( stream, encoding );
        updateMetadata( groupId, artifactId, version, json );
    }

    public void updateMetadata( final String groupId, final String artifactId, final String version, final String json )
        throws AproxWorkflowException
    {
        final TypeToken<Map<String, String>> tt = new TypeToken<Map<String, String>>()
        {
        };

        final Map<String, String> metadata = serializer.fromString( json, tt );

        if ( metadata == null || metadata.isEmpty() )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "No metadata updates found in request body!" );
        }

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

        logger.info( "Adding metadata for: {}\n\n  ", ref, join( metadata.entrySet(), "\n  " ) );

        ops.updateMetadata( ref, metadata );
    }

    public String getCollation( final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        final MetadataCollationDTO dto = configHelper.readCollationDTO( configStream, encoding );
        return getCollation( dto );
    }

    public String getCollation( final String json )
        throws AproxWorkflowException
    {
        final MetadataCollationDTO dto = configHelper.readCollationDTO( json );
        return getCollation( dto );
    }

    private String getCollation( final MetadataCollationDTO dto )
        throws AproxWorkflowException
    {
        try
        {
            final MetadataCollation result = ops.collate( dto );
            return result == null ? null : serializer.toString( result );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to resolve or collate graph contents by metadata: {}. Reason: {}", e, dto, e.getMessage() );
        }
    }

}
