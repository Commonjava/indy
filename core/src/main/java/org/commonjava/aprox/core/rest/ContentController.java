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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    public static final String LISTING_FILE = "index.html";

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

        final List<ConcreteResource> listed = fileManager.list( store, path );
        final Map<String, Set<String>> listingUrls = new TreeMap<String, Set<String>>();

        final String storeUrl = uriFormatter.formatAbsolutePathTo( serviceUrl, type.singularEndpointName(), name );

        // first pass, process only obvious directory entries (ending in '/')
        // second pass, process the remainder.
        for ( int pass = 0; pass < 2; pass++ )
        {
            for ( final ConcreteResource res : listed )
            {
                String p = res.getPath();
                if ( pass == 0 && !p.endsWith( "/" ) )
                {
                    continue;
                }
                else if ( pass == 1 )
                {
                    if ( !p.endsWith( "/" ) )
                    {
                        final String dirpath = p + "/";
                        if ( listingUrls.containsKey( normalize( storeUrl, dirpath ) ) )
                        {
                            p = dirpath;
                        }
                    }
                    else
                    {
                        continue;
                    }
                }

                final String localUrl = normalize( storeUrl, p );
                Set<String> sources = listingUrls.get( localUrl );
                if ( sources == null )
                {
                    sources = new HashSet<String>();
                    listingUrls.put( localUrl, sources );
                }

                sources.add( normalize( res.getLocationUri(), res.getPath() ) );
            }
        }

        final List<String> sources = new ArrayList<String>();
        for ( final ConcreteResource res : listed )
        {
            final String uri = normalize( res.getLocation()
                                             .getUri(), path );
            if ( !sources.contains( uri ) )
            {
                sources.add( uri );
            }
        }

        Collections.sort( sources );

        String parentPath = normalize( normalize( parentPath( normalize( parentPath( path ) ) ) ), LISTING_FILE );
        String parentUrl = uriFormatter.formatAbsolutePathTo( serviceUrl, type.singularEndpointName(), name, parentPath );
        if ( parentPath.equals( LISTING_FILE ) )
        {
            parentPath = null;
            parentUrl = null;
        }

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put( "items", listingUrls );
        params.put( "parentUrl", parentUrl );
        params.put( "parentPath", parentPath );
        params.put( "path", path );
        params.put( "storeKey", key );
        params.put( "storeUrl", storeUrl );
        params.put( "baseUrl", serviceUrl );
        params.put( "sources", sources );

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
