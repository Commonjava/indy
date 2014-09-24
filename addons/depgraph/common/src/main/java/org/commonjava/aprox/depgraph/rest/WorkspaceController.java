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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.dto.GraphWorkspaceDTO;
import org.commonjava.aprox.dto.CreationDTO;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.cartographer.data.CartoGraphUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class WorkspaceController
{

    @Inject
    private RelationshipGraphFactory graphFactory;

    @Inject
    private ObjectMapper serializer;

    public void delete( final String id )
        throws AproxWorkflowException
    {
        try
        {
            if ( !graphFactory.deleteWorkspace( id ) )
            {
                throw new AproxWorkflowException( "Delete failed for workspace: {}", id );
            }
        }
        catch ( final RelationshipGraphException e )
        {
            throw new AproxWorkflowException( "Error deleting workspace: {}. Reason: {}", e, id, e.getMessage() );
        }
    }

    public CreationDTO createNamed( final String id, final String serviceUrl, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        RelationshipGraph graph = null;
        try
        {
            if ( graphFactory.workspaceExists( id ) )
            {
                throw new AproxWorkflowException( "Workspace already exists: {}", id );
            }

            graph = graphFactory.open( new ViewParams( id ), true );

            final String json = serializer.writeValueAsString( new GraphWorkspaceDTO( graph ) );

            return new CreationDTO( new URI( uriFormatter.formatAbsolutePathTo( serviceUrl, graph.getWorkspaceId() ) ),
                                    json );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new AproxWorkflowException( "Failed to create new workspace: {}", e, e.getMessage() );
        }
        catch ( final URISyntaxException e )
        {
            throw new AproxWorkflowException( "Failed to generate location URI for: {}. Reason: {}", e, id,
                                              e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public CreationDTO create( final String serviceUrl, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final String id = System.currentTimeMillis() + ".db";
        return createNamed( id, serviceUrl, uriFormatter );
    }

    public CreationDTO createFrom( final String serviceUrl, final UriFormatter uriFormatter,
                                   final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        ViewParams params;
        try
        {
            params = serializer.readValue( configStream, ViewParams.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read DTO from JSON: %s", e, e.getMessage() );
        }

        return createFrom( serviceUrl, uriFormatter, params );
    }

    public CreationDTO createFrom( final String serviceUrl, final UriFormatter uriFormatter, final String json )
        throws AproxWorkflowException
    {
        ViewParams params;
        try
        {
            params = serializer.readValue( json, ViewParams.class );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read DTO from JSON: %s", e, e.getMessage() );
        }

        return createFrom( serviceUrl, uriFormatter, params );
    }

    public CreationDTO createFrom( final String serviceUrl, final UriFormatter uriFormatter, final ViewParams params )
        throws AproxWorkflowException
    {
        RelationshipGraph graph = null;
        try
        {
            if ( graphFactory.workspaceExists( params.getWorkspaceId() ) )
            {
                throw new AproxWorkflowException( "Workspace already exists: {}", params.getWorkspaceId() );
            }

            graph = graphFactory.open( params, true );

            final String json = serializer.writeValueAsString( graph );

            return new CreationDTO( new URI( uriFormatter.formatAbsolutePathTo( serviceUrl, graph.getWorkspaceId() ) ),
                                    json );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new AproxWorkflowException( "Failed to create new workspace: ", e, e.getMessage() );
        }
        catch ( final URISyntaxException e )
        {
            throw new AproxWorkflowException( "Failed to generate location URI for: {}. Reason: {}", e,
                                              graph.getWorkspaceId(), e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }
    }

    public String get( final String id )
        throws AproxWorkflowException
    {
        RelationshipGraph graph = null;
        try
        {
            graph = graphFactory.open( new ViewParams( id ), false );
            return graph == null ? null : serializer.writeValueAsString( graph );
        }
        catch ( final RelationshipGraphException e )
        {
            throw new AproxWorkflowException( "Failed to load workspace: {}. Reason: {}", e, id, e.getMessage() );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }
        finally
        {
            CartoGraphUtils.closeGraphQuietly( graph );
        }

    }

    public String list()
        throws AproxWorkflowException
    {
        final Set<String> graph = graphFactory.listWorkspaces();
        try
        {
            return graph == null || graph.isEmpty() ? null
                            : serializer.writeValueAsString( Collections.singletonMap( "items", graph ) );
        }
        catch ( final JsonProcessingException e )
        {
            throw new AproxWorkflowException( "Failed to serialize to JSON: %s", e, e.getMessage() );
        }
    }


}
