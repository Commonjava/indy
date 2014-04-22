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

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.dto.CreationDTO;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.ops.WorkspaceOps;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class WorkspaceController
{

    @Inject
    private WorkspaceOps ops;

    @Inject
    @DepgraphSpecific
    private JsonSerializer serializer;

    public void delete( final String id )
        throws AproxWorkflowException
    {
        try
        {
            if ( !ops.delete( id ) )
            {
                throw new AproxWorkflowException( "Delete failed for workspace: {}", id );
            }
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Error deleting workspace: {}. Reason: {}", e, id, e.getMessage() );
        }
    }

    public CreationDTO createNamed( final String id, final String serviceUrl, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        GraphWorkspace ws = null;
        try
        {
            ws = ops.create( id, new GraphWorkspaceConfiguration() );

            final String json = serializer.toString( ws );

            return new CreationDTO( new URI( uriFormatter.formatAbsolutePathTo( serviceUrl, ws.getId() ) ), json );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to create new workspace: {}", e, e.getMessage() );
        }
        catch ( final URISyntaxException e )
        {
            throw new AproxWorkflowException( "Failed to generate location URI for: {}. Reason: {}", e, id, e.getMessage() );
        }
        finally
        {
            detach( ws );
        }
    }

    public CreationDTO create( final String serviceUrl, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        GraphWorkspace ws = null;
        try
        {
            ws = ops.create( new GraphWorkspaceConfiguration() );

            final String json = serializer.toString( ws );

            return new CreationDTO( new URI( uriFormatter.formatAbsolutePathTo( serviceUrl, ws.getId() ) ), json );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to create new workspace: ", e, e.getMessage() );
        }
        catch ( final URISyntaxException e )
        {
            throw new AproxWorkflowException( "Failed to generate location URI for: {}. Reason: {}", e, ws.getId(), e.getMessage() );
        }
        finally
        {
            detach( ws );
        }
    }

    public CreationDTO createFrom( final String serviceUrl, final UriFormatter uriFormatter, final InputStream configStream, final String encoding )
        throws AproxWorkflowException
    {
        final GraphWorkspaceConfiguration config = serializer.fromStream( configStream, encoding, GraphWorkspaceConfiguration.class );
        return createFrom( serviceUrl, uriFormatter, config );
    }

    public CreationDTO createFrom( final String serviceUrl, final UriFormatter uriFormatter, final String json )
        throws AproxWorkflowException
    {
        final GraphWorkspaceConfiguration config = serializer.fromString( json, GraphWorkspaceConfiguration.class );
        return createFrom( serviceUrl, uriFormatter, config );
    }

    public CreationDTO createFrom( final String serviceUrl, final UriFormatter uriFormatter, final GraphWorkspaceConfiguration config )
        throws AproxWorkflowException
    {
        GraphWorkspace ws = null;
        try
        {
            ws = ops.create( config );

            final String json = serializer.toString( ws );

            return new CreationDTO( new URI( uriFormatter.formatAbsolutePathTo( serviceUrl, ws.getId() ) ), json );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to create new workspace: ", e, e.getMessage() );
        }
        catch ( final URISyntaxException e )
        {
            throw new AproxWorkflowException( "Failed to generate location URI for: {}. Reason: {}", e, ws.getId(), e.getMessage() );
        }
        finally
        {
            detach( ws );
        }
    }

    public String get( final String id )
        throws AproxWorkflowException
    {
        GraphWorkspace ws;
        try
        {
            ws = ops.get( id );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to load workspace: {}. Reason: {}", e, id, e.getMessage() );
        }

        if ( ws != null )
        {
            ws.detach();
        }

        return ws == null ? null : serializer.toString( ws );
    }

    public String list()
    {
        final Set<GraphWorkspace> ws = ops.list();
        return ws == null || ws.isEmpty() ? null : serializer.toString( new Listing<GraphWorkspace>( ws ) );
    }

    public boolean addSource( final String id, final String source, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        GraphWorkspace ws = null;
        try
        {
            ws = ops.get( id );
            if ( ws == null )
            {
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find workspace: {}", id );
            }

            return ops.addSource( source, ws );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to load workspace: {}. Reason: {}", e, id, e.getMessage() );
        }
        finally
        {
            detach( ws );
        }
    }

    public boolean addPomLocation( final String id, final String profile, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        GraphWorkspace ws = null;
        try
        {
            ws = ops.get( id );
            if ( ws == null )
            {
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find workspace: {}", id );
            }

            return ops.addProfile( profile, ws );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to load workspace: {}. Reason: {}", e, id, e.getMessage() );
        }
        finally
        {
            detach( ws );
        }
    }

    private void detach( final GraphWorkspace ws )
    {
        if ( ws != null )
        {
            ws.detach();
        }
    }
}
