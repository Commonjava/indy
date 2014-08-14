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
package org.commonjava.aprox.core.ctl;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.util.ContentUtils.dedupeListing;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.ContentProducer;
import org.commonjava.aprox.content.FileManager;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.template.AproxGroovyException;
import org.commonjava.aprox.subsys.template.TemplatingEngine;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;

@ApplicationScoped
public class ContentController
{

    public static final String LISTING_HTML_FILE = "index.html";

    private static final int MAX_PEEK_COUNT = 100;

    public static final String HTML_TAG_PATTERN = ".*\\<(!DOCTYPE|[-_.a-zA-Z0-9]+).*";

    private static final int MAX_PEEK_BYTES = 16384;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private FileManager fileManager;

    @Inject
    private Instance<ContentProducer> contentProducerInstances;

    private Set<ContentProducer> contentProducers;

    @Inject
    private TemplatingEngine templates;

    protected ContentController()
    {
    }

    public ContentController( final StoreDataManager storeManager, final FileManager fileManager,
                              final TemplatingEngine templates, final Set<ContentProducer> contentProducers )
    {
        this.storeManager = storeManager;
        this.fileManager = fileManager;
        this.templates = templates;
        this.contentProducers = contentProducers == null ? new HashSet<ContentProducer>() : contentProducers;
    }

    @PostConstruct
    public void initialize()
    {
        contentProducers = new HashSet<ContentProducer>();
        if ( contentProducerInstances != null )
        {
            for ( final ContentProducer producer : contentProducerInstances )
            {
                contentProducers.add( producer );
            }
        }
    }

    public ApplicationStatus delete( final StoreType type, final String name, final String path )
        throws AproxWorkflowException
    {
        return delete( new StoreKey( type, name ), path );
    }

    public ApplicationStatus delete( final StoreKey key, final String path )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( key );

        final boolean deleted = fileManager.delete( store, path );
        return deleted ? ApplicationStatus.OK : ApplicationStatus.NOT_FOUND;
    }

    public Transfer get( final StoreType type, final String name, final String path )
        throws AproxWorkflowException
    {
        return get( new StoreKey( type, name ), path );
    }

    public Transfer get( final StoreKey key, final String path )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( key );
        Transfer item = fileManager.retrieve( store, path );

        if ( item == null )
        {
            for ( final ContentProducer producer : contentProducers )
            {
                item = producer.produceFileContent( store, path );
                if ( item != null )
                {
                    break;
                }
            }
        }

        if ( item == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "{}",
                                              ( path + ( item == null ? " was not found." : "is a directory" ) ) );
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
        return store( new StoreKey( type, name ), path, stream );
    }

    public Transfer store( final StoreKey key, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( key );
        final Transfer item = fileManager.store( store, path, stream, TransferOperation.UPLOAD );

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
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR,
                                              "Failed to retrieve list of concrete stores. Reason: {}", e,
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
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR,
                                              "Failed to retrieve list of concrete stores. Reason: {}", e,
                                              e.getMessage() );
        }
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
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Cannot retrieve store: {}. Reason: {}",
                                              e, key, e.getMessage() );
        }

        if ( store == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find store: {}", key );
        }

        return store;
    }

    public String renderListing( final String acceptHeader, final StoreType type, final String name, final String path,
                                 final String serviceUrl,
                                 final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final StoreKey key = new StoreKey( type, name );
        return renderListing( acceptHeader, key, path, serviceUrl, uriFormatter );
    }

    public String renderListing( final String acceptHeader, final StoreKey key, final String path,
                                 final String serviceUrl,
                                 final UriFormatter uriFormatter )
        throws AproxWorkflowException
    {
        final List<StoreResource> listed = getListing( key, path );

        final Map<String, Set<String>> listingUrls = new TreeMap<String, Set<String>>();

        final String storeUrl =
            uriFormatter.formatAbsolutePathTo( serviceUrl, key.getType()
                                                              .singularEndpointName(), key.getName() );

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

        String parentPath = normalize( parentPath( normalize( parentPath( path ) ) ) ) + "/";

        String parentUrl =
            uriFormatter.formatAbsolutePathTo( serviceUrl, key.getType()
                                                              .singularEndpointName(), key.getName(), parentPath );

        if ( parentPath.equals( "//" ) || parentPath.equals( "/" ) )
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
            return templates.render( acceptHeader, "directory-listing", params );
        }
        catch ( final AproxGroovyException e )
        {
            throw new AproxWorkflowException( e.getMessage(), e );
        }
    }

    public List<StoreResource> getListing( final StoreType type, final String name, final String path )
        throws AproxWorkflowException
    {
        return getListing( new StoreKey( type, name ), path );
    }

    public List<StoreResource> getListing( final StoreKey key, final String path )
        throws AproxWorkflowException
    {
        final ArtifactStore store = getStore( key );

        List<StoreResource> listed = fileManager.list( store, path );
        boolean added = false;
        for ( final ContentProducer producer : contentProducers )
        {
            final List<StoreResource> produced = producer.produceDirectoryContent( store, path );
            if ( produced != null )
            {
                listed.addAll( produced );
                added = true;
            }
        }

        if ( added )
        {
            listed = dedupeListing( listed );
        }

        return listed;
    }

    public boolean isHtmlContent( final Transfer item )
        throws AproxWorkflowException
    {
        final byte[] head = new byte[MAX_PEEK_BYTES];
        BufferedReader reader = null;
        InputStream raw = null;
        try
        {
            raw = item.openInputStream( false );
            final int read = raw.read( head );
            if ( read > 0 )
            {
                reader = new BufferedReader( new InputStreamReader( new ByteArrayInputStream( head ) ) );

                String line = null;
                int count = 0;
                while ( ( line = reader.readLine() ) != null && count < MAX_PEEK_COUNT )
                {
                    if ( line.matches( HTML_TAG_PATTERN ) )
                    {
                        return true;
                    }
                    count++;
                }
            }
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Cannot read: %s. Reason: %s", e, item, e.getMessage() );
        }
        finally
        {
            closeQuietly( raw );
            closeQuietly( reader );
        }

        return false;
    }

}
