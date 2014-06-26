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
package org.commonjava.aprox.core.filer;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.change.event.AproxFileEventManager;
import org.commonjava.aprox.change.event.ArtifactStoreRescanEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.rest.group.GroupPathHandler;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.aprox.util.PathUtils;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.ListingResult;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.model.VirtualResource;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class DefaultFileManager
    implements FileManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<GroupPathHandler> groupHandlerInstances;

    private Set<GroupPathHandler> groupHandlers;

    @Inject
    private Event<ArtifactStoreRescanEvent> rescanEvent;

    @Inject
    private AproxFileEventManager fileEventManager;

    // Byte, because it's small, and we really only care about the keys anyway.
    private final Map<StoreKey, Byte> rescansInProgress = new ConcurrentHashMap<StoreKey, Byte>();

    @Inject
    @ExecutorConfig( priority = 10, threads = 2, named = "file-manager" )
    private ExecutorService executor; // = Executors.newFixedThreadPool( 8 );

    @Inject
    private TransferManager transfers;

    @Inject
    private LocationExpander locationExpander;

    @Inject
    private StoreDataManager storeManager;

    protected DefaultFileManager()
    {
    }

    public DefaultFileManager( final StoreDataManager storeManager, final TransferManager transfers,
                               final LocationExpander locationExpander, final Collection<GroupPathHandler> groupHandlers )
    {
        this.storeManager = storeManager;
        this.transfers = transfers;
        this.locationExpander = locationExpander;
        this.fileEventManager = new AproxFileEventManager();
        executor = Executors.newFixedThreadPool( 10 );

        this.groupHandlers = new LinkedHashSet<GroupPathHandler>();
        if ( groupHandlers != null )
        {
            this.groupHandlers.addAll( groupHandlers );
        }
    }

    @PostConstruct
    public void cdiInit()
    {
        groupHandlers = new LinkedHashSet<GroupPathHandler>();
        if ( groupHandlerInstances != null )
        {
            for ( final GroupPathHandler h : groupHandlerInstances )
            {
                groupHandlers.add( h );
            }
        }
    }

    @Override
    public List<ConcreteResource> list( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        final String dir = PathUtils.dirname( path );

        final List<ConcreteResource> result = new ArrayList<ConcreteResource>();
        if ( store.getKey()
                  .getType() == StoreType.group )
        {
            try
            {
                final List<ListingResult> results =
                    transfers.listAll( locationExpander.expand( new VirtualResource(
                                                                                     LocationUtils.toLocations( store ),
                                                                                     dir ) ) );

                for ( final ListingResult lr : results )
                {
                    for ( final String file : lr.getListing() )
                    {
                        result.add( new ConcreteResource( lr.getLocation(), dir, file ) );
                    }
                }
            }
            catch ( final TransferException e )
            {
                logger.error( e.getMessage(), e );
                throw new AproxWorkflowException( "Failed to list ALL paths: {} from: {}. Reason: {}", e, path,
                                                  store.getKey(), e.getMessage() );
            }
        }
        else
        {
            final Location loc = LocationUtils.toLocation( store );
            final ConcreteResource res = new ConcreteResource( loc, dir );
            if ( store instanceof RemoteRepository )
            {
                try
                {
                    final ListingResult lr = transfers.list( res );
                    if ( lr != null )
                    {
                        for ( final String file : lr.getListing() )
                        {
                            result.add( new ConcreteResource( loc, dir, file ) );
                        }
                    }
                }
                catch ( final TransferException e )
                {
                    logger.error( e.getMessage(), e );
                    throw new AproxWorkflowException( "Failed to list path: {} from: {}. Reason: {}", e, path,
                                                      store.getKey(), e.getMessage() );
                }
            }
            else
            {
                try
                {
                    final ListingResult listing = transfers.list( res );
                    for ( final String child : listing.getListing() )
                    {
                        result.add( new ConcreteResource( loc, child ) );
                    }
                }
                catch ( final TransferException e )
                {
                    logger.error( e.getMessage(), e );
                    throw new AproxWorkflowException( "Failed to list path: {} from: {}. Reason: {}", e, path,
                                                      store.getKey(), e.getMessage() );
                }
            }
        }

        return dedupeListing( result );
    }

    @Override
    public List<ConcreteResource> list( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        final String dir = PathUtils.dirname( path );

        final List<ConcreteResource> result = new ArrayList<ConcreteResource>();
        try
        {
            final List<ListingResult> results =
                transfers.listAll( locationExpander.expand( new VirtualResource( LocationUtils.toLocations( stores ),
                                                                                 path ) ) );

            for ( final ListingResult lr : results )
            {
                for ( final String file : lr.getListing() )
                {
                    result.add( new ConcreteResource( lr.getLocation(), dir, file ) );
                }
            }
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new AproxWorkflowException( "Failed to list ALL paths: {} from: {}. Reason: {}", e, path, stores,
                                              e.getMessage() );
        }

        return dedupeListing( result );
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        try
        {
            return transfers.retrieveFirst( locationExpander.expand( new VirtualResource(
                                                                                          LocationUtils.toLocations( stores ),
                                                                                          path ) ) );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new AproxWorkflowException( "Failed to retrieve first path: {} from: {}. Reason: {}", e, path,
                                              stores, e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#downloadAll(java.util.List, java.lang.String)
     */
    @Override
    public Set<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        try
        {
            // FIXME: Needs to be a list?
            return new HashSet<Transfer>(
                                          transfers.retrieveAll( locationExpander.expand( new VirtualResource(
                                                                                                               LocationUtils.toLocations( stores ),
                                                                                                               path ) ) ) );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new AproxWorkflowException( "Failed to retrieve ALL paths: {} from: {}. Reason: {}", e, path, stores,
                                              e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#download(org.commonjava.aprox.core.model.ArtifactStore,
     * java.lang.String)
     */
    @Override
    public Transfer retrieve( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        return retrieve( store, path, false );
    }

    private Transfer retrieve( final ArtifactStore store, final String path, final boolean suppressFailures )
        throws AproxWorkflowException
    {
        if ( store.getKey()
                  .getType() == StoreType.group )
        {
            return groupRetrieve( (Group) store, path, suppressFailures );
        }

        Transfer target = null;
        try
        {
            final ConcreteResource res = new ConcreteResource( LocationUtils.toLocation( store ), path );
            if ( store instanceof RemoteRepository )
            {
                target = transfers.retrieve( res );
            }
            else
            {
                target = transfers.getCacheReference( res );
            }

            return target;
            //            if ( target != null && target.exists() )
            //            {
            //                //                logger.info( "Using stored copy from artifact store: {} for: {}", store.getName(), path );
            //                final Transfer item = getStorageReference( store.getKey(), path );
            //
            //                return item;
            //            }
            //            else
            //            {
            //                return null;
            //            }
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new AproxWorkflowException( "Failed to retrieve path: {} from: {}. Reason: {}", e, path, store,
                                              e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#upload(org.commonjava.aprox.core.model.DeployPoint,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public Transfer store( final ArtifactStore store, final String path, final InputStream stream,
                           final TransferOperation op )
        throws AproxWorkflowException
    {
        if ( store.getKey()
                  .getType() == StoreType.group )
        {
            return groupStore( (Group) store, path, stream, op );
        }

        if ( store.getKey()
                  .getType() != StoreType.hosted )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST,
                                              "Cannot deploy to non-deploy point artifact store: {}.", store.getKey() );
        }

        final HostedRepository deploy = (HostedRepository) store;

        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo != null && pathInfo.isSnapshot() )
        {
            if ( !deploy.isAllowSnapshots() )
            {
                logger.error( "Cannot store snapshot in non-snapshot deploy point: {}", deploy.getName() );
                throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST,
                                                  "Cannot store snapshot in non-snapshot deploy point: {}",
                                                  deploy.getName() );
            }
        }
        else if ( !deploy.isAllowReleases() )
        {
            logger.error( "Cannot store release in snapshot-only deploy point: {}", deploy.getName() );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST,
                                              "Cannot store release in snapshot-only deploy point: {}",
                                              deploy.getName() );
        }

        final Transfer target = getStorageReference( deploy, path );

        // TODO: Need some protection for released files!
        // if ( target.exists() )
        // {
        // throw new WebApplicationException(
        // Response.status( Status.BAD_REQUEST ).entity( "Deployment path already exists." ).build() );
        // }

        OutputStream out = null;
        try
        {
            out = target.openOutputStream( op, false );
            copy( stream, out );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to store: %s in deploy store: %s. Reason: %s", path, deploy.getName(),
                                         e.getMessage() ), e );

            throw new AproxWorkflowException( "Failed to store: {} in deploy store: {}. Reason: {}", e, path,
                                              deploy.getName(), e.getMessage() );
        }
        finally
        {
            closeQuietly( out );
        }

        return target;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#upload(java.util.List, java.lang.String,
     * java.io.InputStream)
     */
    @Override
    public Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream,
                           final TransferOperation op )
        throws AproxWorkflowException
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        HostedRepository selected = null;
        for ( final ArtifactStore store : stores )
        {
            if ( store instanceof HostedRepository )
            {
                //                logger.info( "Found deploy point: %s", store.getName() );
                final HostedRepository dp = (HostedRepository) store;
                if ( pathInfo == null )
                {
                    // probably not an artifact, most likely metadata instead...
                    //                    logger.info( "Selecting it for non-artifact storage: {}", path );
                    selected = dp;
                    break;
                }
                else if ( pathInfo.isSnapshot() )
                {
                    if ( dp.isAllowSnapshots() )
                    {
                        //                        logger.info( "Selecting it for snapshot storage: {}", pathInfo );
                        selected = dp;
                        break;
                    }
                }
                else if ( dp.isAllowReleases() )
                {
                    //                    logger.info( "Selecting it for release storage: {}", pathInfo );
                    selected = dp;
                    break;
                }
            }
        }

        if ( selected == null )
        {
            logger.warn( "Cannot deploy. No valid deploy points in group." );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "No deployment locations available." );
        }

        store( selected, path, stream, op );

        return getStorageReference( selected.getKey(), path );
    }

    @Override
    public ArtifactPathInfo parsePathInfo( final String path )
    {
        if ( isEmpty( path ) || path.endsWith( "/" ) )
        {
            return null;
        }

        final String[] parts = path.split( "/" );
        if ( parts.length < 4 )
        {
            return null;
        }

        final String file = parts[parts.length - 1];
        final String version = parts[parts.length - 2];
        final String artifactId = parts[parts.length - 3];
        final StringBuilder groupId = new StringBuilder();
        for ( int i = 0; i < parts.length - 3; i++ )
        {
            if ( groupId.length() > 0 )
            {
                groupId.append( '.' );
            }

            groupId.append( parts[i] );
        }

        return new ArtifactPathInfo( groupId.toString(), artifactId, version, file, path );
    }

    @Override
    public Transfer getStoreRootDirectory( final StoreKey key )
        throws AproxWorkflowException
    {
        ArtifactStore store;
        try
        {
            store = storeManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve ArtifactStore for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }

        if ( store == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find store: {}", key );
        }

        return transfers.getStoreRootDirectory( LocationUtils.toLocation( store ) );
    }

    @Override
    public Transfer getStoreRootDirectory( final ArtifactStore store )
    {
        return transfers.getStoreRootDirectory( LocationUtils.toLocation( store ) );
    }

    @Override
    public Transfer getStorageReference( final ArtifactStore store, final String... path )
    {
        return transfers.getCacheReference( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
    }

    @Override
    public Transfer getStorageReference( final StoreKey key, final String... path )
        throws AproxWorkflowException
    {
        ArtifactStore store;
        try
        {
            store = storeManager.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve ArtifactStore for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }

        if ( store == null )
        {
            throw new AproxWorkflowException( ApplicationStatus.NOT_FOUND, "Cannot find store: {}", key );
        }

        return transfers.getCacheReference( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
    }

    @Override
    public boolean deleteAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        boolean result = false;
        for ( final ArtifactStore store : stores )
        {
            result = delete( store, path ) || result;
        }

        return result;
    }

    @Override
    public boolean delete( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        if ( store.getKey()
                  .getType() == StoreType.group )
        {
            return groupDelete( (Group) store, path );
        }

        final Transfer item = getStorageReference( store, path == null ? ROOT_PATH : path );
        return doDelete( item );
    }

    private Boolean doDelete( final Transfer item )
        throws AproxWorkflowException
    {
        try
        {
            transfers.delete( item.getResource() );
        }
        catch ( final TransferException e )
        {
            throw new AproxWorkflowException( "Failed to delete: {}. Reason: {}", e, item, e.getMessage() );
        }

        return true;
    }

    @Override
    public void rescanAll( final List<? extends ArtifactStore> stores )
        throws AproxWorkflowException
    {
        for ( final ArtifactStore store : stores )
        {
            rescan( store );
        }
    }

    protected Transfer groupRetrieve( final Group store, final String path, final boolean suppressFailures )
        throws AproxWorkflowException
    {
        List<ArtifactStore> stores;
        try
        {
            stores = storeManager.getOrderedConcreteStoresInGroup( store.getName() );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR,
                                              "Failed to lookup membership of group: '{}'. Reason: {}'", e,
                                              store.getKey(), e.getMessage() );
        }

        for ( final GroupPathHandler handler : groupHandlers )
        {

            if ( handler.canHandle( path ) )
            {
                //                logger.info( "Retrieving path: {} using GroupPathHandler: {}", path, handler.getClass()
                //                                                                                            .getName() );
                return handler.retrieve( store, stores, path );
            }
        }

        return retrieveFirst( stores, path );
    }

    protected Transfer groupStore( final Group store, final String path, final InputStream stream,
                                   final TransferOperation op )
        throws AproxWorkflowException
    {
        List<ArtifactStore> stores;
        try
        {
            stores = storeManager.getOrderedConcreteStoresInGroup( store.getName() );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR,
                                              "Failed to lookup membership of group: '{}'. Reason: {}'", e,
                                              store.getKey(), e.getMessage() );
        }

        for ( final GroupPathHandler handler : groupHandlers )
        {

            if ( handler.canHandle( path ) )
            {
                //                logger.info( "Retrieving path: {} using GroupPathHandler: {}", path, handler.getClass()
                //                                                                                            .getName() );
                return handler.store( store, stores, path, stream );
            }
        }

        return store( stores, path, stream, op );
    }

    protected boolean groupDelete( final Group store, final String path )
        throws AproxWorkflowException
    {
        List<ArtifactStore> stores;
        try
        {
            stores = storeManager.getOrderedConcreteStoresInGroup( store.getName() );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR,
                                              "Failed to lookup membership of group: '{}'. Reason: {}'", e,
                                              store.getKey(), e.getMessage() );
        }

        for ( final GroupPathHandler handler : groupHandlers )
        {

            if ( handler.canHandle( path ) )
            {
                //                logger.info( "Retrieving path: {} using GroupPathHandler: {}", path, handler.getClass()
                //                                                                                            .getName() );
                return handler.delete( store, stores, path );
            }
        }

        return deleteAll( stores, path );
    }

    @Override
    public void rescan( final ArtifactStore store )
        throws AproxWorkflowException
    {
        executor.execute( new Rescanner( getStorageReference( store.getKey() ), rescansInProgress, fileEventManager,
                                         rescanEvent ) );
    }

    private static final class Rescanner
        implements Runnable
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        private static final Byte IN_PROGRESS_FLAG = (byte) 0x1;

        private final Map<StoreKey, Byte> rescansInProgress;

        private final Transfer start;

        private final Event<ArtifactStoreRescanEvent> rescanEvent;

        private final AproxFileEventManager fileEventManager;

        public Rescanner( final Transfer start, final Map<StoreKey, Byte> rescansInProgress,
                          final AproxFileEventManager fileEventManager,
                          final Event<ArtifactStoreRescanEvent> rescanEvent )
        {
            this.start = start;
            this.rescansInProgress = rescansInProgress;
            this.fileEventManager = fileEventManager;
            this.rescanEvent = rescanEvent;
        }

        @Override
        public void run()
        {
            final KeyedLocation kl = (KeyedLocation) start.getLocation();
            final StoreKey storeKey = kl.getKey();
            synchronized ( rescansInProgress )
            {
                if ( rescansInProgress.containsKey( storeKey ) )
                {
                    return;
                }

                rescansInProgress.put( storeKey, IN_PROGRESS_FLAG );
            }

            try
            {
                if ( rescanEvent != null )
                {
                    rescanEvent.fire( new ArtifactStoreRescanEvent( kl.getKey() ) );
                }

                doRescan( start );
            }
            finally
            {
                synchronized ( rescansInProgress )
                {
                    rescansInProgress.remove( storeKey );
                }
            }
        }

        private void doRescan( final Transfer item )
        {
            if ( !item.exists() )
            {
                return;
            }

            if ( item.isDirectory() )
            {
                try
                {
                    final String[] listing = item.list();
                    for ( final String sub : listing )
                    {
                        doRescan( item.getChild( sub ) );
                    }
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to list local contents: {}. Reason: {}", e, item, e.getMessage() );
                }
            }

            fileEventManager.fire( new FileAccessEvent( item ) );
        }

    }

    /**
     * Attempt to remove duplicates, even "fuzzy" ones where a directory is 
     * listed with trailing '/' in some cases but not others.
     */
    private List<ConcreteResource> dedupeListing( final List<ConcreteResource> listing )
    {
        final List<ConcreteResource> result = new ArrayList<ConcreteResource>();
        final Map<String, ConcreteResource> mapping = new LinkedHashMap<String, ConcreteResource>();
        for ( final ConcreteResource res : listing )
        {
            final String path = res.getPath();
            if ( mapping.containsKey( path ) )
            {
                continue;
            }

            if ( mapping.containsKey( path + "/" ) )
            {
                continue;
            }

            mapping.put( path, res );
            result.add( res );
        }

        return result;
    }

}
