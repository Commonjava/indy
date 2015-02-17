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
package org.commonjava.aprox.depgraph.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.dto.MetadataBatchUpdateDTO;
import org.commonjava.aprox.depgraph.dto.MetadataCollationDTO;
import org.commonjava.aprox.depgraph.dto.MetadataUpdateDTO;
import org.commonjava.aprox.depgraph.util.ConfigDTOHelper;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.dto.MetadataCollation;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class MetadataController
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MetadataOps ops;

    @Inject
    private ObjectMapper serializer;

    @Inject
    private ConfigDTOHelper configHelper;

    public void batchUpdate( final InputStream stream, final String encoding, final String workspaceId )
        throws AproxWorkflowException
    {
        final String json = readJson( stream, encoding );
        batchUpdate( json, workspaceId );
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
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Cannot read metadata mapping JSON from stream: {}", e, e.getMessage() );
        }
    }

    public void batchUpdate( final String json, final String workspaceId )
        throws AproxWorkflowException
    {
        MetadataBatchUpdateDTO dto;
        try
        {
            dto = serializer.readValue( json, MetadataBatchUpdateDTO.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to deserialize DTO from JSON: %s", e, e.getMessage() );
        }

        if ( dto == null || dto.isEmpty() )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_MODIFIED.code(),
                                              "No changes found in metadata request." );
        }

        final ViewParams params = new ViewParams( workspaceId );
        for ( final Map.Entry<ProjectVersionRef, MetadataUpdateDTO> entry : dto )
        {
            final ProjectVersionRef ref = entry.getKey();
            final Map<String, String> metadata = entry.getValue()
                                                      .getUpdates();

            logger.debug( "Adding metadata for: {}\n\n  ", ref, new JoinString( "\n  ", metadata.entrySet() ) );
            try
            {
                ops.updateMetadata( ref, metadata, params );
            }
            catch ( final CartoDataException e )
            {
                throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "Cannot update metadata: %s",
                                                  e,
                                                  e.getMessage() );
            }
        }
    }

    public String getMetadata( final String groupId, final String artifactId, final String version,
                               final String workspaceId )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        Map<String, String> metadata = null;
        try
        {
            metadata = ops.getMetadata( ref, new ViewParams( workspaceId ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve metadata map for: {}. Reason: {}", e, ref,
                                              e.getMessage() );
        }

        try
        {
            return metadata == null ? null : serializer.writeValueAsString( metadata );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to render JSON: %s", e, e.getMessage() );
        }
    }

    public String getMetadataValue( final String groupId, final String artifactId, final String version,
                                    final String key, final String workspaceId )
        throws AproxWorkflowException
    {
        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );
        try
        {
            final String value = ops.getMetadataValue( ref, key, new ViewParams( workspaceId ) );
            return value == null ? null : serializer.writeValueAsString( Collections.singletonMap( key, value ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve metadata map for: {}. Reason: {}", e, ref,
                                              e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to render JSON: %s", e, e.getMessage() );
        }
    }

    public void updateMetadata( final String groupId, final String artifactId, final String version,
                                final InputStream stream, final String encoding, final String workspaceId )
        throws AproxWorkflowException
    {
        final String json = readJson( stream, encoding );
        updateMetadata( groupId, artifactId, version, json, workspaceId );
    }

    public void updateMetadata( final String groupId, final String artifactId, final String version, final String json,
                                final String workspaceId )
        throws AproxWorkflowException
    {
        MetadataUpdateDTO metadata;
        try
        {
            metadata = serializer.readValue( json, MetadataUpdateDTO.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to parse JSON for update DTO: %s", e, e.getMessage() );
        }

        if ( metadata == null || metadata.isEmpty() )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "No metadata updates found in request body!" );
        }

        final ProjectVersionRef ref = new ProjectVersionRef( groupId, artifactId, version );

        logger.debug( "Adding metadata for: {}\n\n  ", ref, new JoinString( "\n  ", metadata.getUpdates()
                                                                                            .entrySet() ) );

        try
        {
            ops.updateMetadata( ref, metadata.getUpdates(), new ViewParams( workspaceId ) );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Cannot update metadata for: '%s:%s:%s'. Reason: %s", e, groupId,
                                              artifactId, version, e.getMessage() );
        }
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
            return result == null ? null : serializer.writeValueAsString( result );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to resolve or collate graph contents by metadata: {}. Reason: {}",
                                              e, dto, e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to render JSON: %s", e, e.getMessage() );
        }
    }

}
