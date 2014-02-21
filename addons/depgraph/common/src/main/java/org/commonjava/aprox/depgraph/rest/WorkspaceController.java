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

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.core.dto.CreationDTO;
import org.commonjava.aprox.core.util.UriFormatter;
import org.commonjava.aprox.depgraph.inject.DepgraphSpecific;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.VersionUtils;
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
                throw new AproxWorkflowException( "Delete failed for workspace: %s", id );
            }
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Error deleting workspace: %s. Reason: %s", e, id, e.getMessage() );
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
            throw new AproxWorkflowException( "Failed to create new workspace: %s", e, e.getMessage() );
        }
        catch ( final URISyntaxException e )
        {
            throw new AproxWorkflowException( "Failed to generate location URI for: %s. Reason: %s", e, id, e.getMessage() );
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
            throw new AproxWorkflowException( "Failed to generate location URI for: %s. Reason: %s", e, ws.getId(), e.getMessage() );
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
            throw new AproxWorkflowException( "Failed to generate location URI for: %s. Reason: %s", e, ws.getId(), e.getMessage() );
        }
        finally
        {
            detach( ws );
        }
    }

    public boolean select( final String id, final String groupId, final String artifactId, final String newVersion, final String oldVersion,
                           final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        GraphWorkspace ws = null;
        boolean modified = false;
        ProjectRef pr = null;
        try
        {
            final SingleVersion ver = VersionUtils.createSingleVersion( newVersion );

            ws = ops.get( id );
            if ( ws == null )
            {
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find workspace: %s", id );
            }

            if ( oldVersion == null )
            {
                pr = new ProjectRef( groupId, artifactId );
                ws.selectVersion( pr, new ProjectVersionRef( pr, ver ) );
                modified = true;
            }
            else
            {
                final ProjectVersionRef orig = new ProjectVersionRef( groupId, artifactId, oldVersion );
                final ProjectVersionRef selected = ws.selectVersion( orig, orig.selectVersion( ver ) );

                modified = selected.equals( orig );
                pr = orig;
            }

            return modified;
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
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
            throw new AproxWorkflowException( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
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
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find workspace: %s", id );
            }

            return ops.addSource( source, ws );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
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
                throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find workspace: %s", id );
            }

            return ops.addProfile( profile, ws );
        }
        catch ( final CartoDataException e )
        {
            throw new AproxWorkflowException( "Failed to load workspace: %s. Reason: %s", e, id, e.getMessage() );
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
