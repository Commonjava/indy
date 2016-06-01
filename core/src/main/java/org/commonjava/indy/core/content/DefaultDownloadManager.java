/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.core.content;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.IndyFileEventManager;
import org.commonjava.indy.change.event.IndyStoreErrorEvent;
import org.commonjava.indy.change.event.ArtifactStoreRescanEvent;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.indy.util.PathUtils;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.galley.BadGatewayException;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferLocationException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.TransferTimeoutException;
import org.commonjava.maven.galley.event.EventMetadata;
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

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.commonjava.indy.util.ContentUtils.dedupeListing;

@javax.enterprise.context.ApplicationScoped
public class DefaultDownloadManager
    implements DownloadManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Event<ArtifactStoreRescanEvent> rescanEvent;

    @Inject
    private IndyFileEventManager fileEventManager;

    // Byte, because it's small, and we really only care about the keys anyway.
    private final Map<StoreKey, Byte> rescansInProgress = new ConcurrentHashMap<>();

    @Inject
    @ExecutorConfig( priority = 10, threads = 2, named = "file-manager" )
    private ExecutorService executor; // = Executors.newFixedThreadPool( 8 );

    @Inject
    private TransferManager transfers;

    @Inject
    private LocationExpander locationExpander;

    @Inject
    private StoreDataManager storeManager;

    protected DefaultDownloadManager()
    {
    }

    public DefaultDownloadManager( final StoreDataManager storeManager, final TransferManager transfers,
                                   final LocationExpander locationExpander )
    {
        this.storeManager = storeManager;
        this.transfers = transfers;
        this.locationExpander = locationExpander;
        this.fileEventManager = new IndyFileEventManager();
        executor = Executors.newFixedThreadPool( 10 );
    }

    @Override
    public List<StoreResource> list( final ArtifactStore store, final String path )
        throws IndyWorkflowException
    {
        final List<StoreResource> result = new ArrayList<>();

        if ( store.isDisabled() )
        {
            return result;
        }

        //        final String dir = PathUtils.dirname( path );

        if ( store.getKey()
                  .getType() == StoreType.group )
        {
            try
            {
                final List<ListingResult> results =
                    transfers.listAll( locationExpander.expand( new VirtualResource(
                                                                                     LocationUtils.toLocations( store ),
                                                                                     path ) ) );

                for ( final ListingResult lr : results )
                {
                    if ( lr != null && lr.getListing() != null )
                    {
                        for ( final String file : lr.getListing() )
                        {
                            result.add( new StoreResource( (KeyedLocation) lr.getLocation(), path, file ) );
                        }
                    }
                }
            }
            catch ( final BadGatewayException e )
            {
                Location location = e.getLocation();
                KeyedLocation kl = (KeyedLocation) location;

                fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
                logger.warn( "Bad gateway: " + e.getMessage(), e );
            }
            catch ( final TransferTimeoutException e )
            {
                Location location = e.getLocation();
                KeyedLocation kl = (KeyedLocation) location;

                fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
                logger.warn( "Timeout: " + e.getMessage(), e );
            }
            catch ( final TransferLocationException e )
            {
                Location location = e.getLocation();
                KeyedLocation kl = (KeyedLocation) location;

                fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
                logger.warn( "Location Error: " + e.getMessage(), e );
            }
            catch ( final TransferException e )
            {
                logger.error( e.getMessage(), e );
                throw new IndyWorkflowException( "Failed to list ALL paths: {} from: {}. Reason: {}", e, path,
                                                  store.getKey(), e.getMessage() );
            }
        }
        else
        {
            final KeyedLocation loc = LocationUtils.toLocation( store );
            final StoreResource res = new StoreResource( loc, path );
            if ( store instanceof RemoteRepository )
            {
                try
                {
                    final ListingResult lr = transfers.list( res );
                    if ( lr != null && lr.getListing() != null )
                    {
                        for ( final String file : lr.getListing() )
                        {
                            result.add( new StoreResource( loc, path, file ) );
                        }
                    }
                }
                catch ( final BadGatewayException e )
                {
                    Location location = e.getLocation();
                    KeyedLocation kl = (KeyedLocation) location;

                    fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
                    logger.warn( "Bad gateway: " + e.getMessage(), e );
                }
                catch ( final TransferTimeoutException e )
                {
                    Location location = e.getLocation();
                    KeyedLocation kl = (KeyedLocation) location;

                    fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
                    logger.warn( "Timeout: " + e.getMessage(), e );
                }
                catch ( final TransferLocationException e )
                {
                    Location location = e.getLocation();
                    KeyedLocation kl = (KeyedLocation) location;

                    fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
                    logger.warn( "Location Error: " + e.getMessage(), e );
                }
                catch ( final TransferException e )
                {
                    logger.error( e.getMessage(), e );
                    throw new IndyWorkflowException( "Failed to list path: {} from: {}. Reason: {}", e, path,
                                                      store.getKey(), e.getMessage() );
                }
            }
            else
            {
                try
                {
                    final ListingResult listing = transfers.list( res );
                    if ( listing != null && listing.getListing() != null )
                    {
                        for ( final String child : listing.getListing() )
                        {
                            result.add( new StoreResource( loc, path, child ) );
                        }
                    }
                }
                catch ( final TransferLocationException e )
                {
                    Location location = res.getLocation();
                    KeyedLocation kl = (KeyedLocation) location;

                    logger.warn( "Timeout  / bad gateway: " + e.getMessage(), e );
                    fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
                }
                catch ( final TransferException e )
                {
                    logger.error( e.getMessage(), e );
                    throw new IndyWorkflowException( "Failed to list path: {} from: {}. Reason: {}", e, path,
                                                      store.getKey(), e.getMessage() );
                }
            }
        }

        return dedupeListing( result );
    }

    @Override
    public List<StoreResource> list( final List<? extends ArtifactStore> stores, final String path )
        throws IndyWorkflowException
    {
        List<ArtifactStore> enabled = findEnabled( stores );
        final String dir = PathUtils.dirname( path );

        final List<StoreResource> result = new ArrayList<>();
        try
        {
            final List<ListingResult> results =
                transfers.listAll( locationExpander.expand( new VirtualResource( LocationUtils.toLocations( enabled ),
                                                                                 path ) ) );

            for ( final ListingResult lr : results )
            {
                if ( lr != null && lr.getListing() != null )
                {
                    for ( final String file : lr.getListing() )
                    {
                        result.add( new StoreResource( (KeyedLocation) lr.getLocation(), dir, file ) );
                    }
                }
            }
        }
        catch ( final BadGatewayException e )
        {
            Location location = e.getLocation();
            KeyedLocation kl = (KeyedLocation) location;

            fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
            logger.warn( "Bad gateway: " + e.getMessage(), e );
        }
        catch ( final TransferTimeoutException e )
        {
            Location location = e.getLocation();
            KeyedLocation kl = (KeyedLocation) location;

            fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
            logger.warn( "Timeout: " + e.getMessage(), e );
        }
        catch ( final TransferLocationException e )
        {
            Location location = e.getLocation();
            KeyedLocation kl = (KeyedLocation) location;

            fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
            logger.warn( "Location Error: " + e.getMessage(), e );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new IndyWorkflowException( "Failed to list ALL paths: {} from: {}. Reason: {}", e, path, stores,
                                              e.getMessage() );
        }

        return dedupeListing( result );
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
        throws IndyWorkflowException
    {
        return retrieveFirst( stores, path, new EventMetadata() );
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path,
                                   final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        List<ArtifactStore> enabled = findEnabled( stores );

        try
        {
            return transfers.retrieveFirst( locationExpander.expand( new VirtualResource(
                                                                                          LocationUtils.toLocations( enabled ),
                                                                                          path ) ), eventMetadata );
        }
        catch ( final BadGatewayException e )
        {
            Location location = e.getLocation();
            KeyedLocation kl = (KeyedLocation) location;

            fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
            logger.warn( "Bad gateway: " + e.getMessage(), e );
        }
        catch ( final TransferTimeoutException e )
        {
            Location location = e.getLocation();
            KeyedLocation kl = (KeyedLocation) location;

            fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
            logger.warn( "Timeout: " + e.getMessage(), e );
        }
        catch ( final TransferLocationException e )
        {
            Location location = e.getLocation();
            KeyedLocation kl = (KeyedLocation) location;

            fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
            logger.warn( "Location Error: " + e.getMessage(), e );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new IndyWorkflowException( "Failed to retrieve first path: {} from: {}. Reason: {}", e, path,
                                              stores, e.getMessage() );
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.indy.core.rest.util.FileManager#downloadAll(java.util.List, java.lang.String)
     */
    @Override
    public List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
        throws IndyWorkflowException
    {
        return retrieveAll( stores, path, new EventMetadata() );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.indy.core.rest.util.FileManager#downloadAll(java.util.List, java.lang.String)
     */
    @Override
    public List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores , final String path , final EventMetadata eventMetadata  )
        throws IndyWorkflowException
    {
        List<ArtifactStore> enabled = findEnabled( stores );
        try
        {
            return transfers.retrieveAll(
                    locationExpander.expand( new VirtualResource( LocationUtils.toLocations( enabled ), path ) ),
                    eventMetadata );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new IndyWorkflowException( "Failed to retrieve ALL paths: {} from: {}. Reason: {}", e, path, stores,
                                              e.getMessage() );
        }
    }

    private List<ArtifactStore> findEnabled( final List<? extends ArtifactStore> stores )
    {
        List<ArtifactStore> enabled = new ArrayList<>( stores.size() );
        for ( ArtifactStore store: stores )
        {
            if ( !store.isDisabled() )
            {
                enabled.add( store );
            }
            else
            {
                logger.debug( "{} is disabled.", store.getKey() );
            }
        }

        return enabled;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.indy.core.rest.util.FileManager#download(org.commonjava.indy.core.model.ArtifactStore,
     * java.lang.String)
     */
    @Override
    public Transfer retrieve( final ArtifactStore store, final String path )
        throws IndyWorkflowException
    {
        return retrieve( store, path, new EventMetadata() );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.indy.core.rest.util.FileManager#download(org.commonjava.indy.core.model.ArtifactStore,
     * java.lang.String)
     */
    @Override
    public Transfer retrieve( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        return retrieve( store, path, false, eventMetadata );
    }

    private Transfer retrieve( final ArtifactStore store, final String path, final boolean suppressFailures,
                               final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        if ( store.isDisabled() )
        {
            return null;
        }

        if ( store.getKey()
                  .getType() == StoreType.group )
        {
            return null;
        }

        Transfer target = null;
        try
        {
            final ConcreteResource res = new ConcreteResource( LocationUtils.toLocation( store ), path );
            if ( store instanceof RemoteRepository )
            {
                target = transfers.retrieve( res, suppressFailures, eventMetadata );
            }
            else
            {
                target = transfers.getCacheReference( res );
                if ( !target.exists() )
                {
                    target = null;
                }
            }

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
        catch ( final TransferLocationException e )
        {
            fileEventManager.fire( new IndyStoreErrorEvent( store.getKey(), e ) );
            logger.warn( "Timeout / bad gateway: " + e.getMessage(), e );
            target = null;
//            throw new IndyWorkflowException( ApplicationStatus.NOT_FOUND.code(),
//                                              "Failed to retrieve path: {} from: {}. Reason: {}", e, path, store,
//                                              e.getMessage() );
        }
        catch ( final TransferException e )
        {
            logger.error( e.getMessage(), e );
            throw new IndyWorkflowException( "Failed to retrieve path: {} from: {}. Reason: {}", e, path, store,
                                              e.getMessage() );
        }

        return target;
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.indy.core.rest.util.FileManager#upload(org.commonjava.indy.core.model.DeployPoint,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public Transfer store( final ArtifactStore store, final String path, final InputStream stream,
                           final TransferOperation op )
        throws IndyWorkflowException
    {
        return store( store, path, stream, op, new EventMetadata() );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.indy.core.rest.util.FileManager#upload(org.commonjava.indy.core.model.DeployPoint,
     * java.lang.String, java.io.InputStream)
     */
    @Override
    public Transfer store( final ArtifactStore store, final String path, final InputStream stream,
                           final TransferOperation op, final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        if ( store.isDisabled() )
        {
            return null;
        }

        if ( store.getKey()
                  .getType() == StoreType.group )
        {
            //FIXME: Why is this null? Investigate.
            return null;
        }

        if ( store.getKey()
                  .getType() != StoreType.hosted )
        {
            throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Cannot deploy to non-deploy point artifact store: {}.", store.getKey() );
        }

        if ( store instanceof HostedRepository )
        {
            final HostedRepository deploy = (HostedRepository) store;

            final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
            if ( pathInfo != null && pathInfo.isSnapshot() )
            {
                if ( !deploy.isAllowSnapshots() )
                {
                    logger.error( "Cannot store snapshot in non-snapshot deploy point: {}", deploy.getName() );
                    throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                                      "Cannot store snapshot in non-snapshot deploy point: {}",
                                                      deploy.getName() );
                }
            }
            else if ( !deploy.isAllowReleases() )
            {
                logger.error( "Cannot store release in snapshot-only deploy point: {}", deploy.getName() );
                throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                                  "Cannot store release in snapshot-only deploy point: {}",
                                                  deploy.getName() );
            }
        }

//        final Transfer target = getStorageReference( deploy, path );

        // TODO: Need some protection for released files!
        // if ( target.exists() )
        // {
        // throw new WebApplicationException(
        // Response.status( Status.BAD_REQUEST ).entity( "Deployment path already exists." ).build() );
        // }

        try
        {
            return transfers.store( new ConcreteResource( LocationUtils.toLocation( store ), path ), stream, eventMetadata );
        }
        catch ( final BadGatewayException e )
        {
            Location location = e.getLocation();
            KeyedLocation kl = (KeyedLocation) location;

            fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
            logger.warn( "Bad gateway: " + e.getMessage(), e );
            throw new IndyWorkflowException( "Failed to store path: {} in: {}. Reason: {}", e, path, store,
                                              e.getMessage() );
        }
        catch ( final TransferTimeoutException e )
        {
            Location location = e.getLocation();
            KeyedLocation kl = (KeyedLocation) location;

            fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
            logger.warn( "Timeout: " + e.getMessage(), e );
            throw new IndyWorkflowException( "Failed to store path: {} in: {}. Reason: {}", e, path, store,
                                              e.getMessage() );
        }
        catch ( final TransferLocationException e )
        {
            Location location = e.getLocation();
            KeyedLocation kl = (KeyedLocation) location;

            fileEventManager.fire( new IndyStoreErrorEvent( kl.getKey(), e ) );
            logger.warn( "Location Error: " + e.getMessage(), e );
            throw new IndyWorkflowException( "Failed to store path: {} in: {}. Reason: {}", e, path, store,
                                              e.getMessage() );
        }
        catch ( TransferException e )
        {
            logger.error(
                    String.format( "Failed to store: %s in: %s. Reason: %s", path, store.getKey(), e.getMessage() ), e );

            throw new IndyWorkflowException( "Failed to store: %s in: %s. Reason: %s", e, path,
                                              store.getKey(), e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.indy.core.rest.util.FileManager#upload(java.util.List, java.lang.String,
     * java.io.InputStream)
     */
    @Override
    public Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream,
                           final TransferOperation op )
        throws IndyWorkflowException
    {
        return store( stores, path, stream, op, new EventMetadata() );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.indy.core.rest.util.FileManager#upload(java.util.List, java.lang.String,
     * java.io.InputStream)
     */
    @Override
    public Transfer store( final List<? extends ArtifactStore> stores , final String path , final InputStream stream ,
                           final TransferOperation op , final EventMetadata eventMetadata  )
        throws IndyWorkflowException
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        HostedRepository selected = null;
        for ( final ArtifactStore store : stores )
        {
            if ( storeIsSuitableFor( store, pathInfo, op ) )
            {
                selected = (HostedRepository) store;
                break;
            }
        }

        if ( selected == null )
        {
            logger.warn( "Cannot deploy. No valid deploy points in group." );
            throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "No deployment locations available." );
        }

        logger.info( "Storing: {} in selected: {} with event metadata: {}", path, selected, eventMetadata );
        store( selected, path, stream, op, eventMetadata );

        return getStorageReference( selected.getKey(), path );
    }

    @Override
    public Transfer getStoreRootDirectory( final StoreKey key )
        throws IndyWorkflowException
    {
        ArtifactStore store;
        try
        {
            store = storeManager.getArtifactStore( key );
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( "Failed to retrieve ArtifactStore for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }

        if ( store == null )
        {
            throw new IndyWorkflowException( ApplicationStatus.NOT_FOUND.code(), "Cannot find store: {}", key );
        }

        return transfers.getStoreRootDirectory( LocationUtils.toLocation( store ) );
    }

    @Override
    public Transfer getStoreRootDirectory( final ArtifactStore store )
    {
        return transfers.getStoreRootDirectory( LocationUtils.toLocation( store ) );
    }

    @Override
    public Transfer getStorageReference( final List<ArtifactStore> stores, final String path, final TransferOperation op )
        throws IndyWorkflowException
    {
        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );

        Transfer transfer = null;
        for ( final ArtifactStore store : stores )
        {
            if ( storeIsSuitableFor( store, pathInfo, op ) )
            {
                logger.info( "Attempting to retrieve storage reference in: {} for: {} (operation: {})", store, path,
                             op );

                transfer = getStorageReference( store, path );
                if ( transfer != null && ( ( op != TransferOperation.DOWNLOAD && op != TransferOperation.LISTING )
                        || transfer.exists() ) )
                {
                    logger.info( "Using transfer: {}", transfer );
                    break;
                }
            }
        }

        if ( transfer == null )
        {
            logger.warn( "No suitable stores in list." );
            throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(), "No suitable store available." );
        }

        return transfer;
    }

    private boolean storeIsSuitableFor( final ArtifactStore store, final ArtifactPathInfo pathInfo,
                                        final TransferOperation op )
    {
        if ( store.isDisabled() )
        {
            return false;
        }

        if ( TransferOperation.UPLOAD == op )
        {
            if ( store instanceof HostedRepository )
            {
                //                logger.info( "Found deploy point: %s", store.getName() );
                final HostedRepository dp = (HostedRepository) store;
                if ( pathInfo == null )
                {
                    // probably not an artifact, most likely metadata instead...
                    //                    logger.info( "Selecting it for non-artifact storage: {}", path );
                    return true;
                }
                else if ( pathInfo.isSnapshot() )
                {
                    if ( dp.isAllowSnapshots() )
                    {
                        //                        logger.info( "Selecting it for snapshot storage: {}", pathInfo );
                        return true;
                    }
                }
                else if ( dp.isAllowReleases() )
                {
                    //                    logger.info( "Selecting it for release storage: {}", pathInfo );
                    return true;
                }
            }
            // TODO: Allow push-through via remote repositories too.
        }
        else
        {
            return true;
        }

        return false;
    }

    @Override
    public Transfer getStorageReference( final ArtifactStore store, final String path, final TransferOperation op )
        throws IndyWorkflowException
    {
        if ( store.isDisabled() )
        {
            return null;
        }

        final ArtifactPathInfo pathInfo = ArtifactPathInfo.parse( path );
        if ( storeIsSuitableFor( store, pathInfo, op ) )
        {
            return getStorageReference( store.getKey(), path );
        }

        logger.warn( "Store {} not suitable for: {}", store, op );
        throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                          "Store is not suitable for this operation." );
    }

    @Override
    public Transfer getStorageReference( final ArtifactStore store, final String... path )
    {
        return getStorageReference( store, false, path );
    }

    @Override
    public Transfer getStorageReference( final ArtifactStore store, final boolean allowDisabled,
                                         final String... path )
    {
        if ( store.isDisabled() && !allowDisabled )
        {
            return null;
        }

        return transfers.getCacheReference( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
    }

    @Override
    public Transfer getStorageReference( final StoreKey key, final String... path )
        throws IndyWorkflowException
    {
        ArtifactStore store;
        try
        {
            store = storeManager.getArtifactStore( key );
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( "Failed to retrieve ArtifactStore for: %s. Reason: %s", e, key,
                                              e.getMessage() );
        }

        if ( store == null )
        {
            throw new IndyWorkflowException( ApplicationStatus.NOT_FOUND.code(), "Cannot find store: {}", key );
        }

        if ( store.isDisabled() )
        {
            return null;
        }

        return transfers.getCacheReference( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
    }

    @Override
    public boolean deleteAll( final List<? extends ArtifactStore> stores, final String path )
        throws IndyWorkflowException
    {
        boolean result = false;
        for ( final ArtifactStore store : stores )
        {
            if ( store.isDisabled() )
            {
                continue;
            }

            result = delete( store, path, new EventMetadata() ) || result;
        }

        return result;
    }

    @Override
    public boolean delete( final ArtifactStore store, final String path )
        throws IndyWorkflowException
    {
        return delete( store, path, new EventMetadata() );
    }

    @Override
    public boolean delete( final ArtifactStore store, final String path, final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        if ( store.isDisabled() )
        {
            return false;
        }

        if ( store.getKey()
                  .getType() == StoreType.group )
        {
            return false;
        }

        final Transfer item = getStorageReference( store, path == null ? ROOT_PATH : path );
        return doDelete( item, eventMetadata );
    }

    private Boolean doDelete( final Transfer item, final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        try
        {
            transfers.delete( item.getResource(), eventMetadata );
        }
        catch ( final TransferException e )
        {
            throw new IndyWorkflowException( "Failed to delete: {}. Reason: {}", e, item, e.getMessage() );
        }

        return true;
    }

    @Override
    public void rescanAll( final List<? extends ArtifactStore> stores )
        throws IndyWorkflowException
    {
        rescanAll( stores, new EventMetadata() );
    }

    @Override
    public void rescanAll( final List<? extends ArtifactStore> stores, final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        for ( final ArtifactStore store : stores )
        {
            rescan( store, eventMetadata );
        }
    }

    @Override
    public void rescan( final ArtifactStore store )
        throws IndyWorkflowException
    {
        rescan( store, new EventMetadata() );
    }

    @Override
    public void rescan( final ArtifactStore store, final EventMetadata eventMetadata )
        throws IndyWorkflowException
    {
        if ( store.isDisabled() )
        {
            return;
        }

        executor.execute( new Rescanner( store, getStorageReference( store.getKey() ), rescansInProgress,
                                         fileEventManager,
 rescanEvent, eventMetadata ) );
    }

    private static final class Rescanner
        implements Runnable
    {
        private final Logger logger = LoggerFactory.getLogger( getClass() );

        private static final Byte IN_PROGRESS_FLAG = (byte) 0x1;

        private final Map<StoreKey, Byte> rescansInProgress;

        private final Transfer start;

        private final Event<ArtifactStoreRescanEvent> rescanEvent;

        private final IndyFileEventManager fileEventManager;

        private final ArtifactStore store;

        private final EventMetadata eventMetadata;

        public Rescanner( final ArtifactStore store, final Transfer start, final Map<StoreKey, Byte> rescansInProgress,
                          final IndyFileEventManager fileEventManager,
                          final Event<ArtifactStoreRescanEvent> rescanEvent, final EventMetadata eventMetadata )
        {
            this.store = store;
            this.start = start;
            this.rescansInProgress = rescansInProgress;
            this.fileEventManager = fileEventManager;
            this.rescanEvent = rescanEvent;
            this.eventMetadata = eventMetadata;
        }

        @Override
        public void run()
        {
            final StoreKey storeKey = store.getKey();
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
                    rescanEvent.fire( new ArtifactStoreRescanEvent( eventMetadata, store ) );
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

            fileEventManager.fire( new FileAccessEvent( item, eventMetadata ) );
        }

    }

    @Override
    public List<Transfer> listRecursively( final StoreKey src, final String startPath )
        throws IndyWorkflowException
    {
        final List<Transfer> result = new ArrayList<>();
        final Transfer transfer = getStorageReference( src, startPath );
        recurseListing( transfer, result );

        return result;
    }

    private void recurseListing( final Transfer transfer, final List<Transfer> result )
        throws IndyWorkflowException
    {
        if ( transfer.isDirectory() )
        {
            try
            {
                final String[] children = transfer.list();
                for ( final String child : children )
                {
                    final Transfer childTransfer = transfer.getChild( child );
                    recurseListing( childTransfer, result );
                }
            }
            catch ( final IOException e )
            {
                throw new IndyWorkflowException( "Failed to list children of: %s. Reason: %s", e, transfer,
                                                  e.getMessage() );
            }
        }
        else if ( transfer.exists() )
        {
            result.add( transfer );
        }
    }

}
