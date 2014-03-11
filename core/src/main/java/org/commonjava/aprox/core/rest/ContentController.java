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
package org.commonjava.aprox.core.rest;

import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.template.AproxGroovyException;
import org.commonjava.aprox.subsys.template.TemplatingEngine;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;

@ApplicationScoped
public class ContentController
{

    private static final Comparator<? super ConcreteResource> ITEMS_LISTING_COMPARATOR = new Comparator<ConcreteResource>()
    {
        @Override
        public int compare( final ConcreteResource f, final ConcreteResource s )
        {
            return f.getPath()
                    .compareTo( s.getPath() );
        }
    };

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private FileManager fileManager;

    @Inject
    private TemplatingEngine templates;

    protected ContentController()
    {
    }

    public ContentController( final StoreDataManager storeManager, final FileManager fileManager )
    {
        this.storeManager = storeManager;
        this.fileManager = fileManager;
    }

    public ApplicationStatus delete( final StoreType type, final String name, final String path )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( type, name );

        final boolean deleted = fileManager.delete( store, path );
        return deleted ? ApplicationStatus.OK : ApplicationStatus.NOT_FOUND;
    }

    public Transfer get( final StoreType type, final String name, final String path )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( type, name );
        final Transfer item = fileManager.retrieve( store, path );

        if ( item == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "{}", ( path + ( item == null ? " was not found." : "is a directory" ) ) );
        }

        return item;
    }

    public String getContentType( final String path )
    {
        return new MimetypesFileTypeMap().getContentType( path );
    }

    public Transfer store( final StoreType type, final String name, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( type, name );
        final Transfer item = fileManager.store( store, path, stream );

        return item;
    }

    public void rescan( final StoreKey key )
        throws AproxWorkflowException
    {
        final ArtifactStore artifactStore = getStore( key );
        fileManager.rescan( artifactStore );
    }

    public void rescanAll()
        throws AproxWorkflowException
    {
        try
        {
            final List<ArtifactStore> stores = storeManager.getAllConcreteArtifactStores();
            fileManager.rescanAll( stores );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve list of concrete stores. Reason: {}", e,
                                              e.getMessage() );
        }
    }

    public void deleteAll( final String path )
        throws AproxWorkflowException
    {
        try
        {
            final List<ArtifactStore> stores = storeManager.getAllConcreteArtifactStores();
            fileManager.deleteAll( stores, path );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to retrieve list of concrete stores. Reason: {}", e,
                                              e.getMessage() );
        }
    }

    private ArtifactStore getStore( final StoreType type, final String name )
        throws AproxWorkflowException
    {
        final StoreKey key = new StoreKey( type, name );
        return getStore( key );
    }

    private ArtifactStore getStore( final StoreKey key )
        throws AproxWorkflowException
    {
        ArtifactStore store = null;
        try
        {
            store = storeManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Cannot retrieve store: {}. Reason: {}", e, key, e.getMessage() );
        }

        if ( store == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find store: {}", key );
        }

        return store;
    }

    public String list( final StoreType type, final String name, final String path, final String serviceUrl, final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final StoreKey key = new StoreKey( type, name );
        final ArtifactStore store = getStore( key );

        List<ConcreteResource> items = fileManager.list( store, path );
        final List<ConcreteResource> deduped = new ArrayList<ConcreteResource>( items.size() );
        for ( final ConcreteResource res : items )
        {
            final String p = res.getPath();
            if ( !p.matches( ".+\\.[^/]+" ) && !p.endsWith( "/" ) )
            {
                final ConcreteResource r = new ConcreteResource( res.getLocation(), p + "/" );
                if ( deduped.contains( r ) )
                {
                    continue;
                }
            }

            if ( !deduped.contains( res ) )
            {
                deduped.add( res );
            }
        }

        items = deduped;
        Collections.sort( items, ITEMS_LISTING_COMPARATOR );

        final String parentPath = normalize( normalize( parentPath( normalize( parentPath( path ) ) ) ), "index.html" );
        final String storeUrl = uriFormatter.formatAbsolutePathTo( serviceUrl, type.singularEndpointName(), name );
        final String parentUrl = uriFormatter.formatAbsolutePathTo( serviceUrl, type.singularEndpointName(), name, parentPath );

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put( "items", items );
        params.put( "parentUrl", parentUrl );
        params.put( "parentPath", parentPath );
        params.put( "path", path );
        params.put( "storeKey", key );
        params.put( "storeUrl", storeUrl );
        params.put( "baseUrl", serviceUrl );

        // render...
        try
        {
            return templates.render( "directory-listing", params );
        }
        catch ( final AproxGroovyException e )
        {
            throw new AproxWorkflowException( e.getMessage(), e );
        }
    }

}
