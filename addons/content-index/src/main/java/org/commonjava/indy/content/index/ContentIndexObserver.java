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
package org.commonjava.indy.content.index;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStoreEnablementEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.core.expire.ContentExpiration;
import org.commonjava.indy.core.expire.ScheduleManager;
import org.commonjava.indy.core.expire.SchedulerEvent;
import org.commonjava.indy.core.expire.SchedulerEventType;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * <p>Maintain the content index in response to events propagating through the system.</p>
 * <p><b>TODO:</b> This observer breaks things subtly.</p>
 * <p>When it removes a metadata file from the index, it also cleans up the Transfers (files) associated with merged
 * content as it propagates index removals up the group inclusion chain. If we roll a distribution that doesn't include
 * this, some merged-metadata problems may come back...</p>
 * <br/>
 * Created by jdcasey on 3/15/16.
 */
@ApplicationScoped
public class ContentIndexObserver
{
    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private SpecialPathManager specialPathManager;

    @Inject
    private DirectContentAccess directContentAccess;

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private ContentIndexManager indexManager;

    @ExecutorConfig( named = "content-indexer", threads = 8, priority = 2, daemon = true )
    @WeftManaged
    @Inject
    private Executor executor;

    protected ContentIndexObserver()
    {
    }

    public ContentIndexObserver( StoreDataManager storeDataManager,
                                 ContentIndexManager indexManager,
                                 SpecialPathManager specialPathManager,
                                 DirectContentAccess directContentAccess,
                                 IndyObjectMapper objectMapper, Executor executor )
    {
        this.storeDataManager = storeDataManager;
        this.indexManager = indexManager;
        this.specialPathManager = specialPathManager;
        this.directContentAccess = directContentAccess;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    public void onFileDeletion( @Observes FileDeletionEvent event )
    {
        StoreKey key = LocationUtils.getKey( event );
        String path = event.getTransfer().getPath();

        AtomicBoolean result = new AtomicBoolean( false );
        indexManager.removeIndexedStorePath( path, key, indexedStorePath -> result.set( true ) );

        if ( result.get() )
        {
            propagateClear( key, path );
        }
    }

    public void onFileAccess( @Observes FileAccessEvent event )
    {
        StoreKey key = LocationUtils.getKey( event );
        indexManager.indexPathInStores( event.getTransfer().getPath(), key );
    }

    public void onFileStorage( @Observes FileStorageEvent event )
    {
        StoreKey key = LocationUtils.getKey( event );
        String path = event.getTransfer().getPath();
        indexManager.indexPathInStores( path, key );

        propagateClear( key, path );
    }

    // FIXME: Re-enable this and fix test failures.
    public void onStoreDisable( @Observes ArtifactStoreEnablementEvent event )
    {
        if ( !event.isDisabling() || !event.isPreprocessing() )
        {
            return;
        }

        propagatePathlessStoreEvent( event );
    }

    public void onStoreDeletion( @Observes ArtifactStoreDeletePreEvent event )
    {
        propagatePathlessStoreEvent( event );
    }

    public void onStoreUpdate( @Observes ArtifactStorePreUpdateEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Got event: {}", event );

        // we're only interested in existing stores, since new stores cannot have indexed keys
        if ( ArtifactStoreUpdateType.UPDATE == event.getType() )
        {
            for ( ArtifactStore store : event )
            {
                removeAllSupercededMemberContent( store, event.getChangeMap() );
            }
        }
    }

    public void invalidateExpiredContent( @Observes final SchedulerEvent event )
    {
        if ( event.getEventType() != SchedulerEventType.TRIGGER || !event.getJobType()
                                                                         .equals( ScheduleManager.CONTENT_JOB_TYPE ) )
        {
            return;
        }

        ContentExpiration expiration = null;
        try
        {
            expiration = objectMapper.readValue( event.getPayload(), ContentExpiration.class );
        }
        catch ( final IOException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Failed to read ContentExpiration from event payload.", e );
        }

        if ( expiration == null )
        {
            return;
        }

        final StoreKey key = expiration.getKey();
        final String path = expiration.getPath();

        AtomicBoolean result = new AtomicBoolean( false );

        // invalidate indexes for the store itself
        indexManager.removeIndexedStorePath( path, key, indexedStorePath -> result.set( true ) );

        // invalidate indexes for groups containing the store
        indexManager.removeOriginIndexedStorePath( path, key, indexedStorePath -> result.set( true ) );

        if ( result.get() )
        {
            propagateClear( key, path );
        }
    }

    // TODO: If we find points where a new HostedRepository is added, we should be using its comprehensive index to minimize the index damage to the group.
    private void removeAllSupercededMemberContent( ArtifactStore store, Map<ArtifactStore, ArtifactStore> changeMap )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        StoreKey key = store.getKey();
        // we're only interested in groups, since only adjustments to group memberships can invalidate indexed content.
        if ( StoreType.group == key.getType() )
        {
            List<StoreKey> newMembers = ( (Group) store ).getConstituents();
            logger.debug( "New members of: {} are: {}", store, newMembers );

            Group group = (Group) changeMap.get( store );
            List<StoreKey> oldMembers = group.getConstituents();
            logger.debug( "Old members of: {} are: {}", group, oldMembers );

            int commonSize = Math.min( newMembers.size(), oldMembers.size() );
            int divergencePoint = -1;

            // look in the members that overlap in new/old groups and see if there are changes that would
            // indicate member reordering. If so, it might lead previously suppressed results to be prioritized,
            // which would invalidate part of the content index for the group.
            for ( divergencePoint = 0; divergencePoint < commonSize; divergencePoint++ )
            {
                logger.debug( "Checking for common member at index: {}", divergencePoint );
                if ( !oldMembers.get( divergencePoint ).equals( newMembers.get( divergencePoint ) ) )
                {
                    break;
                }
            }

            // if we haven't found a reordering of membership, let's look to see if membership has shrunk
            // if it has just grown, we don't care.
            if ( divergencePoint < 0 && newMembers.size() < oldMembers.size() )
            {
                divergencePoint = commonSize;
            }

            logger.debug( "group membership divergence point: {}", divergencePoint );

            // if we can iterate some old members that have been removed or reordered, invalidate the
            // group content index entries for those.
            if ( divergencePoint < oldMembers.size() )
            {
                Set<IndexedStorePath> removed = new HashSet<>();
                for ( int i = divergencePoint; i < oldMembers.size(); i++ )
                {
                    StoreKey memberKey = oldMembers.get( i );
                    indexManager.removeAllOriginIndexedPathsForStore( memberKey, indexedStorePath -> removed.add( indexedStorePath ) );
                }

                propagatePathRemovals( removed );
            }
        }
    }

    private void propagatePathlessStoreEvent( Iterable<ArtifactStore> stores )
    {
        executor.execute(()->{
            stores.forEach( (store)->{
                final StoreKey key = store.getKey();

                Set<String> paths = new HashSet<>();
                indexManager.removeAllIndexedPathsForStore( key, indexedStorePath ->{paths.add(indexedStorePath.getPath());} );
                indexManager.removeAllOriginIndexedPathsForStore( key, indexedStorePath ->{paths.add(indexedStorePath.getPath());} );

                if ( !paths.isEmpty() )
                {
                    try
                    {
                        Set<Group> groups = storeDataManager.getGroupsContaining( key );
                        Location location = LocationUtils.toLocation( store );
                        paths.forEach( (path)->{
                            // here we care if it's mergable or not, since this may be triggered by a new file being stored.
                            SpecialPathInfo specialPathInfo = specialPathManager.getSpecialPathInfo( location, path );
                            if ( specialPathInfo != null && specialPathInfo.isMergable() )
                            {
                                indexManager.clearIndexedPathFrom( path, groups, deleteTransfers() );
                            }
                        } );
                    }
                    catch ( IndyDataException e )
                    {
                        Logger logger = LoggerFactory.getLogger( getClass() );
                        logger.error( String.format( "Failed to lookup groups containing: %s. Reason: %s", key, e.getMessage() ),
                                      e );
                    }
                }
            } );
        });
    }

    private void propagatePathRemovals( Set<IndexedStorePath> removals )
    {
        executor.execute( ()->{
            Map<StoreKey, Set<Group>> containersForKey = new HashMap<>();
            removals.forEach( indexedStorePath -> {
                StoreKey storeKey = indexedStorePath.getStoreKey();
                try
                {
                    ArtifactStore store = storeDataManager.getArtifactStore( storeKey );

                    Set<Group> groups = containersForKey.get( storeKey );
                    if ( groups == null )
                    {
                        groups = storeDataManager.getGroupsContaining( storeKey );
                    }

                    Location location = LocationUtils.toLocation( store );
                    String path = indexedStorePath.getPath();

                    // If the file is stored local to the group, it's merged and must die.
                    indexManager.clearIndexedPathFrom( path, groups, deleteTransfers() );
                }
                catch ( IndyDataException e )
                {
                    Logger logger = LoggerFactory.getLogger( getClass() );
                    logger.error( String.format( "Failed to lookup store, or groups containing store: %s. Reason: %s", storeKey, e.getMessage() ),
                                  e );
                }
            } );
        });
    }

    private void propagateClear( StoreKey key, String path )
    {
        executor.execute(()->{
            try
            {
                Set<Group> groups = storeDataManager.getGroupsContaining( key );

                // the only time a group will have local storage of the path is when it has been merged
                // ...in which case we should try to delete it.
                indexManager.clearIndexedPathFrom( path, groups, deleteTransfers() );
            }
            catch ( IndyDataException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( String.format( "Failed to lookup groups containing: %s. Reason: %s", key, e.getMessage() ),
                              e );
            }
        });
    }

    private Consumer<IndexedStorePath> deleteTransfers()
    {
        return isp ->{
            StoreKey key = isp.getStoreKey();
            String path = isp.getPath();

            try
            {
                Transfer transfer = directContentAccess.getTransfer( key, path );
                if ( transfer != null && transfer.exists() )
                {
                    transfer.delete();
                }
            }
            catch ( IndyWorkflowException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( String.format( "Failed to retrieve Transfer for: %s in store: %s. Reason: %s", path,
                                             key, e.getMessage() ), e );
            }
            catch ( IOException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( String.format( "Failed to delete Transfer for: %s in store: %s. Reason: %s", path,
                                             path, e.getMessage() ), e );
            }
        };
    }

}
