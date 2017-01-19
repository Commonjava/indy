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
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStoreEnablementEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.core.expire.ContentExpiration;
import org.commonjava.indy.core.expire.ScheduleManager;
import org.commonjava.indy.core.expire.SchedulerEvent;
import org.commonjava.indy.core.expire.SchedulerTriggerEvent;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    private static final String ORIGIN_KEY = "ContentIndex:originKey";

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

    protected ContentIndexObserver()
    {
    }

    public ContentIndexObserver( final StoreDataManager storeDataManager, final ContentIndexManager indexManager,
                                 final SpecialPathManager specialPathManager,
                                 final DirectContentAccess directContentAccess, final IndyObjectMapper objectMapper)
    {
        this.storeDataManager = storeDataManager;
        this.indexManager = indexManager;
        this.specialPathManager = specialPathManager;
        this.directContentAccess = directContentAccess;
        this.objectMapper = objectMapper;
    }

    public void onFileDeletion( @Observes final FileDeletionEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got file-deletion event: {}", event );

        StoreKey key = LocationUtils.getKey( event );
        String path = event.getTransfer().getPath();

        AtomicBoolean result = new AtomicBoolean( false );
        indexManager.removeIndexedStorePath( path, key, indexedStorePath -> result.set( true ) );

        if ( result.get() )
        {
            propagateClear( key, path );
        }
    }

    public void onFileAccess( @Observes final FileAccessEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got file-access event: {}", event );

        StoreKey key = LocationUtils.getKey( event );
        indexManager.indexPathInStores( event.getTransfer().getPath(), key );
    }

    public void onFileStorage( @Observes final FileStorageEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got file-storage event: {}", event );

        StoreKey key = LocationUtils.getKey( event );
        String path = event.getTransfer().getPath();
        indexManager.indexPathInStores( path, key );

        propagateClear( key, path );
    }

    public void onStoreDisable( @Observes final ArtifactStoreEnablementEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got store-enablement event: {}", event );

        if ( event.isPreprocessing() )
        {
            if ( event.isDisabling() )
            {
                propagatePathlessStoreEvent( event );
            }
            else
            {
                propagateStoreEnablementEvent( event );
            }
        }

    }

    public void onStoreDeletion( @Observes final ArtifactStoreDeletePostEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got store-delete event: {}", event );

        propagatePathlessStoreEvent( event );
    }

    public void onStoreUpdate( @Observes final ArtifactStorePreUpdateEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got store-update event: {}", event );

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
        if ( !( event instanceof SchedulerTriggerEvent ) || !event.getJobType()
                                                             .equals( ScheduleManager.CONTENT_JOB_TYPE ) )
        {
            return;
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got content-expiration event: {}", event );

        ContentExpiration expiration = null;
        try
        {
            expiration = objectMapper.readValue( event.getPayload(), ContentExpiration.class );
        }
        catch ( final IOException e )
        {
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
    private void removeAllSupercededMemberContent( final ArtifactStore store,
                                                   final Map<ArtifactStore, ArtifactStore> changeMap )
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
            int divergencePoint;

            // look in the members that overlap in new/old groups and see if there are changes that would
            // indicate member reordering. If so, it might lead previously suppressed results to be prioritized,
            // which would invalidate part of the content index for the group.
            boolean foundDivergence = false;
            for ( divergencePoint = 0; divergencePoint < commonSize; divergencePoint++ )
            {
                logger.debug( "Checking for common member at index: {}", divergencePoint );
                if ( !oldMembers.get( divergencePoint ).equals( newMembers.get( divergencePoint ) ) )
                {
                    foundDivergence = true;
                    break;
                }
            }

            // [NOS-128]
            // 1. If membership has shrunk, we can remove origin-indexed paths, which will remove merged group content
            //      based on the removed member's content.
            // 2. If membership has grown, we should iterate new members' indexed content looking for mergable paths.
            //      For each of these, we need to removeIndexedStorePaths using the group and the mergable path.
            // [addendum]
            // 3. If membership is the same size but has been reordered, we need to iterate from the divergence point
            //    and invalidate the non-mergable files. This is because the reordering may change what artifacts
            //    should obscure which other physical artifacts.
            //
            // NOTE: In any case, once we isolate the changes, we need to handle matches in two ways:
            // 1. deleteTransfers()
            // 2. add the indexedStorePath to the removed Set so we can propagage their removal through any groups
            //      that include the one we're affecting directly here...using clearIndexedPathFrom() to do this.
            if ( !foundDivergence )
            {
                if ( newMembers.size() < oldMembers.size() )
                {
                    divergencePoint = commonSize;
                }
                else
                {
                    divergencePoint = newMembers.size();
                }
            }

            logger.debug( "group membership divergence point: {}", divergencePoint );

            Set<StoreKey> affectedMembers = new HashSet<>();
            boolean removeMergableOnly = divergencePoint >= oldMembers.size();

            // if we can iterate some old members that have been removed or reordered, invalidate the
            // group content index entries for those.
            if ( divergencePoint < oldMembers.size() )
            {
                for ( int i = divergencePoint; i < oldMembers.size(); i++ )
                {
                    affectedMembers.add( oldMembers.get( i ) );
                }
            }
            else
            {
                // for new added members, need to clear the indexed path with this group store for repo metadata merging
                // See [NOS-128]
                for ( int i = divergencePoint - 1; i >= commonSize; i-- )
                {
                    affectedMembers.add( newMembers.get( i ) );
                }
            }

            logger.debug( "Got members affected by membership divergence: {}", affectedMembers );
            if ( !affectedMembers.isEmpty() )
            {
                Set<Group> groups = storeDataManager.getGroupsAffectedBy( group.getKey() );
                groups.add( group );

                logger.debug( "Got affected groups: {}", groups );
                Set<String> paths = new HashSet<>();
                affectedMembers.forEach( ( memberKey ) -> paths.addAll(
                        indexManager.getAllIndexedPathsForStore( memberKey )
                                    .stream()
                                    .filter( removeMergableOnly ? mergableStorePaths() : ( p ) -> true )
                                    .map( isp -> isp.getPath() )
                                    .collect( Collectors.toSet() ) ) );

                paths.parallelStream()
                     .forEach( p -> indexManager.clearIndexedPathFrom( p, groups, deleteTransfers() ) );
            }
        }
    }

    private void propagatePathlessStoreEvent( final Iterable<ArtifactStore> stores )
    {
        StreamSupport.stream( stores.spliterator(), true).forEach( ( store ) -> {
                final StoreKey key = store.getKey();

                Set<String> paths = new HashSet<>();
                indexManager.removeAllIndexedPathsForStore( key, indexedStorePath -> {
                    paths.add( indexedStorePath.getPath() );
                } );
                indexManager.removeAllOriginIndexedPathsForStore( key, indexedStorePath -> {
                    paths.add( indexedStorePath.getPath() );
                } );

                if ( !paths.isEmpty() )
                {
                    Set<Group> groups = storeDataManager.getGroupsAffectedBy( key );

                    paths.stream()
                         .filter( mergablePathStrings() )
                         .forEach( ( path ) -> indexManager.clearIndexedPathFrom( path, groups, deleteTransfers() ) );
                }
            } );
    }

    private void propagateStoreEnablementEvent( final Iterable<ArtifactStore> stores )
    {
        StreamSupport.stream( stores.spliterator(), true).forEach( ( store ) -> {
                final StoreKey key = store.getKey();
                if ( key.getType() == StoreType.hosted )
                {
                    // for hosted repos we need to delete from containing groups all mergable content present in the
                    // hosted repo's index, because it is always complete and up-to-date (if initial indexing is
                    // finished)

                    List<IndexedStorePath> indexedStorePaths = indexManager.getAllIndexedPathsForStore( key );

                    if ( indexedStorePaths != null && !indexedStorePaths.isEmpty() )
                    {
                        Set<Group> groups = storeDataManager.getGroupsAffectedBy( key );

                        indexedStorePaths.stream().filter( mergableStorePaths() ).forEach( ( indexedStorePath ) -> {
                            String path = indexedStorePath.getPath();
                            indexManager.clearIndexedPathFrom( path, groups, deleteTransfers() );
                        } );
                    }
                }
                else
                {
                    // for remote repos and groups we need to delete from containing groups ALL mergable content,
                    // because we don't have a complete index for it and building it is too expensive, so we cannot
                    // be sure if the resulting mergable content would be influenced by the newly added/enabled repo
                    // or not
                    try
                    {
                        Set<Group> groups = storeDataManager.getGroupsAffectedBy( key );
                        if ( !groups.isEmpty() )
                        {
                            groups.parallelStream().forEach( ( group ) -> {
                                List<IndexedStorePath> paths =
                                        indexManager.getAllIndexedPathsForStore( group.getKey() );
                                if ( paths != null )
                                {
                                    paths.stream()
                                         .filter( mergableStorePaths() )
                                         .forEach( path -> indexManager.clearIndexedPathFrom( path.getPath(), groups,
                                                                                              deleteTransfers() ) );
                                }
                            } );
                        }
                    }
                    catch ( Exception e )
                    {
                        Logger logger = LoggerFactory.getLogger( getClass() );
                        logger.error( String.format( "Failed to lookup groups containing: %s. Reason: %s", key,
                                                     e.getMessage() ), e );
                    }
                }
            } );
    }

//    private void propagatePathRemovals( final Set<IndexedStorePath> removals )
//    {
//        Map<StoreKey, Set<Group>> containersForKey = new HashMap<>();
//        removals.stream().filter( mergableStorePaths() ).forEach( indexedStorePath -> {
//            StoreKey storeKey = indexedStorePath.getStoreKey();
//            try
//            {
//                ArtifactStore store = storeDataManager.getArtifactStore( storeKey );
//
//                Set<Group> groups = containersForKey.get( storeKey );
//                if ( groups == null )
//                {
//                    groups = storeDataManager.getGroupsAffectedBy( storeKey );
//                    containersForKey.put( storeKey, groups );
//                }
//
//                String path = indexedStorePath.getPath();
//
//                // If the file is stored local to the group, it's merged and must die.
//                indexManager.clearIndexedPathFrom( path, groups, deleteTransfers() );
//            }
//            catch ( IndyDataException e )
//            {
//                Logger logger = LoggerFactory.getLogger( getClass() );
//                logger.error(
//                        String.format( "Failed to lookup store, or groups containing store: %s. Reason: %s", storeKey,
//                                       e.getMessage() ), e );
//            }
//        } );
//    }

    private void propagateClear( final StoreKey key, final String path )
    {
        // here we care if it's mergable or not, since this may be triggered by a new file being stored.
        SpecialPathInfo specialPathInfo = specialPathManager.getSpecialPathInfo( path );
        if ( specialPathInfo != null && specialPathInfo.isMergable() )
        {
            //        executor.execute(()-> {
            ThreadContext context = ThreadContext.getContext( true );
            context.put( ORIGIN_KEY, key );
            try
            {
                Set<Group> groups = storeDataManager.getGroupsAffectedBy( key );

                // the only time a group will have local storage of the path is when it has been merged
                // ...in which case we should try to delete it.
                indexManager.clearIndexedPathFrom( path, groups, deleteTransfers() );
            }
            finally
            {
                context.remove( ORIGIN_KEY );
            }
            //        });
        }
    }

    private Predicate<? super String> mergablePathStrings()
    {
        return ( path ) -> {
            // here we care if it's mergable or not, since this may be triggered by a new file being stored.
            SpecialPathInfo specialPathInfo = specialPathManager.getSpecialPathInfo( path );
            return ( specialPathInfo != null && specialPathInfo.isMergable() );
        };
    }

    private Predicate<? super IndexedStorePath> mergableStorePaths()
    {
        return ( path ) -> {
            // here we care if it's mergable or not, since this may be triggered by a new file being stored.
            SpecialPathInfo specialPathInfo = specialPathManager.getSpecialPathInfo( path.getPath() );
            return ( specialPathInfo != null && specialPathInfo.isMergable() );
        };
    }

    private Consumer<IndexedStorePath> deleteTransfers()
    {
        return isp -> {
            StoreKey key = isp.getStoreKey();
            String path = isp.getPath();

            try
            {
                //                Logger logger = LoggerFactory.getLogger( getClass() );
                Transfer transfer = directContentAccess.getTransfer( key, path );
                //                logger.trace( "retrieving transfer for deletion: {}", transfer );
                if ( transfer != null && transfer.exists() )
                {
                    //                    logger.trace( "DELETE: {}", transfer );

                    // FIXME: We probably need to find some way to notify the system of what we're up to.
                    // However, doing this per file is WAY too expensive!
                    transfer.delete( false );
                }
            }
            catch ( IndyWorkflowException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( String.format( "Failed to retrieve Transfer for: %s in store: %s. Reason: %s", path, key,
                                             e.getMessage() ), e );
            }
            catch ( IOException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( String.format( "Failed to delete Transfer for: %s in store: %s. Reason: %s", path, path,
                                             e.getMessage() ), e );
            }
        };
    }

}
