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

import org.commonjava.aprox.change.event.AproxFileEventManager;
import org.commonjava.aprox.change.event.ArtifactStoreRescanEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.filer.PathUtils;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.rest.util.ApplicationStatus;
import org.commonjava.aprox.rest.util.ArtifactPathInfo;
import org.commonjava.aprox.rest.util.retrieve.GroupPathHandler;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.cdi.util.weft.ExecutorConfig;
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
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class DefaultFileManager
    implements FileManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private Instance<GroupPathHandler> groupHandlerInstances;

    private Set<GroupPathHandler> groupHandlers;

    @Inject
    private Event<ArtifactStoreRescanEvent> rescanEvent;

    @Inject
    private AproxFileEventManager fileEventManager;

    //    @Inject
    //    private NotFoundCache nfc;

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

    public DefaultFileManager( final StoreDataManager storeManager, final TransferManager transfers, final LocationExpander locationExpander,
                               final Collection<GroupPathHandler> groupHandlers )
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
                    transfers.listAll( locationExpander.expand( new VirtualResource( LocationUtils.toLocations( store ), dir ) ) );

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
                throw new AproxWorkflowException( "Failed to list ALL paths: %s from: %s. Reason: %s", e, path, store.getKey(), e.getMessage() );
            }
        }
        else
        {
            final Location loc = LocationUtils.toLocation( store );
            final ConcreteResource res = new ConcreteResource( loc, dir );
            if ( store instanceof Repository )
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
                    throw new AproxWorkflowException( "Failed to list path: %s from: %s. Reason: %s", e, path, store.getKey(), e.getMessage() );
                }
            }
            else
            {
                final Transfer transfer = transfers.getCacheReference( res );
                final String[] files = transfer.list();
                for ( final String file : files )
                {
                    result.add( new ConcreteResource( loc, dir, file ) );
                }
            }
        }

        return result;
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
                transfers.listAll( locationExpander.expand( new VirtualResource( LocationUtils.toLocations( stores ), path ) ) );

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
            throw new AproxWorkflowException( "Failed to list ALL paths: %s from: %s. Reason: %s", e, path, stores, e.getMessage() );
        }

        return result;
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        try
        {
            return transfers.retrieveFirst( locationExpander.expand( new VirtualResource( LocationUtils.toLocations( stores ), path ) ) );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new AproxWorkflowException( "Failed to retrieve first path: %s from: %s. Reason: %s", e, path, stores, e.getMessage() );
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
            return new HashSet<Transfer>( transfers.retrieveAll( locationExpander.expand( new VirtualResource( LocationUtils.toLocations( stores ),
                                                                                                               path ) ) ) );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new AproxWorkflowException( "Failed to retrieve ALL paths: %s from: %s. Reason: %s", e, path, stores, e.getMessage() );
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
            if ( store instanceof Repository )
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
            //                //                logger.info( "Using stored copy from artifact store: %s for: %s", store.getName(), path );
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
            throw new AproxWorkflowException( "Failed to retrieve path: %s from: %s. Reason: %s", e, path, store, e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.aprox.core.rest.util.FileManager#upload(org.commonjava.aprox.core.model.DeployPoint,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public Transfer store( final ArtifactStore store, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        if ( store.getKey()
                  .getType() == StoreType.group )
        {
            return groupStore( (Group) store, path, stream );
        }

        if ( store.getKey()
                  .getType() != StoreType.deploy_point )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Cannot deploy to non-deploy point artifact store: %s.", store.getKey() );
        }

        final DeployPoint deploy = (DeployPoint) store;

        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( pathInfo != null && pathInfo.isSnapshot() )
        {
            if ( !deploy.isAllowSnapshots() )
            {
                logger.error( "Cannot store snapshot in non-snapshot deploy point: %s", deploy.getName() );
                throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Cannot store snapshot in non-snapshot deploy point: %s",
                                                  deploy.getName() );
            }
        }
        else if ( !deploy.isAllowReleases() )
        {
            logger.error( "Cannot store release in snapshot-only deploy point: %s", deploy.getName() );
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST, "Cannot store release in snapshot-only deploy point: %s",
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
            out = target.openOutputStream( TransferOperation.UPLOAD, false );
            copy( stream, out );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to store: %s in deploy store: %s. Reason: %s", e, path, deploy.getName(), e.getMessage() );

            throw new AproxWorkflowException( "Failed to store: %s in deploy store: %s. Reason: %s", e, path, deploy.getName(), e.getMessage() );
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
    public Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        DeployPoint selected = null;
        for ( final ArtifactStore store : stores )
        {
            if ( store instanceof DeployPoint )
            {
                //                logger.info( "Found deploy point: %s", store.getName() );
                final DeployPoint dp = (DeployPoint) store;
                if ( pathInfo == null )
                {
                    // probably not an artifact, most likely metadata instead...
                    //                    logger.info( "Selecting it for non-artifact storage: %s", path );
                    selected = dp;
                    break;
                }
                else if ( pathInfo.isSnapshot() )
                {
                    if ( dp.isAllowSnapshots() )
                    {
                        //                        logger.info( "Selecting it for snapshot storage: %s", pathInfo );
                        selected = dp;
                        break;
                    }
                }
                else if ( dp.isAllowReleases() )
                {
                    //                    logger.info( "Selecting it for release storage: %s", pathInfo );
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

        store( selected, path, stream );

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
    {
        return transfers.getStoreRootDirectory( LocationUtils.toCacheLocation( key ) );
    }

    @Override
    public Transfer getStorageReference( final ArtifactStore store, final String... path )
    {
        return transfers.getCacheReference( new ConcreteResource( LocationUtils.toCacheLocation( store.getKey() ), path ) );
    }

    @Override
    public Transfer getStorageReference( final StoreKey key, final String... path )
    {
        return transfers.getCacheReference( new ConcreteResource( LocationUtils.toCacheLocation( key ), path ) );
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
        if ( !item.exists() )
        {
            return false;
        }

        if ( item.isDirectory() )
        {
            final String[] listing = item.list();
            for ( final String sub : listing )
            {
                if ( !doDelete( item.getChild( sub ) ) )
                {
                    return false;
                }
            }
        }
        else
        {
            try
            {
                if ( !item.delete() )
                {
                    throw new AproxWorkflowException( "Failed to delete: %s.", item );
                }
            }
            catch ( final IOException e )
            {
                throw new AproxWorkflowException( "Failed to delete stored location: %s. Reason: %s", e, item, e.getMessage() );
            }
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
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to lookup membership of group: '%s'. Reason: %s'", e,
                                              store.getKey(), e.getMessage() );
        }

        for ( final GroupPathHandler handler : groupHandlers )
        {

            if ( handler.canHandle( path ) )
            {
                //                logger.info( "Retrieving path: %s using GroupPathHandler: %s", path, handler.getClass()
                //                                                                                            .getName() );
                return handler.retrieve( store, stores, path );
            }
        }

        return retrieveFirst( stores, path );
    }

    protected Transfer groupStore( final Group store, final String path, final InputStream stream )
        throws AproxWorkflowException
    {
        List<ArtifactStore> stores;
        try
        {
            stores = storeManager.getOrderedConcreteStoresInGroup( store.getName() );
        }
        catch ( final ProxyDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to lookup membership of group: '%s'. Reason: %s'", e,
                                              store.getKey(), e.getMessage() );
        }

        for ( final GroupPathHandler handler : groupHandlers )
        {

            if ( handler.canHandle( path ) )
            {
                //                logger.info( "Retrieving path: %s using GroupPathHandler: %s", path, handler.getClass()
                //                                                                                            .getName() );
                return handler.store( store, stores, path, stream );
            }
        }

        return store( stores, path, stream );
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
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR, "Failed to lookup membership of group: '%s'. Reason: %s'", e,
                                              store.getKey(), e.getMessage() );
        }

        for ( final GroupPathHandler handler : groupHandlers )
        {

            if ( handler.canHandle( path ) )
            {
                //                logger.info( "Retrieving path: %s using GroupPathHandler: %s", path, handler.getClass()
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
        executor.execute( new Rescanner( getStorageReference( store.getKey() ), rescansInProgress, fileEventManager, rescanEvent ) );
    }

    private static final class Rescanner
        implements Runnable
    {
        private static final Byte IN_PROGRESS_FLAG = (byte) 0x1;

        private final Map<StoreKey, Byte> rescansInProgress;

        private final Transfer start;

        private final Event<ArtifactStoreRescanEvent> rescanEvent;

        private final AproxFileEventManager fileEventManager;

        public Rescanner( final Transfer start, final Map<StoreKey, Byte> rescansInProgress, final AproxFileEventManager fileEventManager,
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
                final String[] listing = item.list();
                for ( final String sub : listing )
                {
                    doRescan( item.getChild( sub ) );
                }
            }

            fileEventManager.fire( new FileAccessEvent( item ) );
        }

    }

}
