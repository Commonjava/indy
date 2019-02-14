/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.change;

import org.commonjava.cdi.util.weft.DrainingExecutorCompletionService;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStoreEnablementEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.change.event.IndyStoreEvent;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.StoreContentAction;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.commonjava.indy.measure.annotation.MetricNamed.DEFAULT;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.maven.galley.util.PathUtils.ROOT;

/**
 * Created by jdcasey on 1/27/17.
 */
@ApplicationScoped
public class StoreContentListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<StoreContentAction> storeContentActions;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private SpecialPathManager specialPathManager;

    @Inject
    private DirectContentAccess directContentAccess;

    @Inject
    private NotFoundCache nfc;

    @Inject
    @WeftManaged
    @ExecutorConfig( threads=20, priority=7, named="content-cleanup" )
    private WeftExecutorService cleanupExecutor;

    /**
     * Handles store disable/enablement.
     */
    @Measure
    public void onStoreEnablement( @Observes final ArtifactStoreEnablementEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got store-enablement event: {}", event );

        if ( event.isPreprocessing() )
        {
            if ( event.isDisabling() )
            {
                /* For disablement, we remove the merged metadata files by paths via listing mergable files in the target repo's local cache.
                 * We do not cache pom/jar files in affected groups so don't worry about them.
                 */
                processAllPaths( event, mergablePathStrings(), false );
            }
            else
            {
                /* When enabling a repo, we have to clean all (mergable) content from containing groups' cache because
                 * we don't have complete listing of the enabled repo and listing it is very slow. The only option how to keep
                 * the cache consistent is to drop everything from it.
                 */
                processAllPathsExt( event, mergablePathStrings() );
            }
        }
    }

    @Measure( timers = @MetricNamed( DEFAULT ) )
    public void onStoreDeletion( @Observes final ArtifactStoreDeletePreEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got store-delete event: {}", event );

        processAllPaths( event, p->true, true );
    }

    @Measure( timers = @MetricNamed( DEFAULT ) )
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

    // TODO: If we find points where a new HostedRepository is added, we should be using its comprehensive index to minimize the index damage to the group.
    private void removeAllSupercededMemberContent( final ArtifactStore store,
                                                   final Map<ArtifactStore, ArtifactStore> changeMap )
    {
        StoreKey key = store.getKey();
        // we're only interested in groups, since only adjustments to group memberships can invalidate indexed content.
        if ( group == key.getType() )
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
                Set<Group> groups = new HashSet<>();
                groups.add( group );

                try
                {
                    groups.addAll( storeDataManager.query()
                                                   .packageType( group.getPackageType() )
                                                   .getGroupsAffectedBy( group.getKey() ) );
                }
                catch ( IndyDataException e )
                {
                    logger.error( String.format( "Cannot retrieve groups affected by: %s. Reason: %s", group.getKey(),
                                                 e.getMessage() ), e );
                }

                logger.debug( "Got affected groups: {}", groups );

                DrainingExecutorCompletionService<Integer> clearService =
                        new DrainingExecutorCompletionService<>( cleanupExecutor );

                final Predicate<? super String> mergableFilter = removeMergableOnly ? mergablePathStrings() : ( p ) -> true;

                // NOTE: We're NOT checking load for this executor, since this is an async process that is critical to
                // data integrity.
                affectedMembers.forEach( ( memberKey ) -> {
                    logger.debug( "Listing all {}paths in: {}", ( removeMergableOnly ? "mergeable " : "" ), memberKey );
                    listPathsAnd( memberKey, mergableFilter, p -> clearService.submit(
                            clearPathProcessor( p, memberKey, groups ) ) );

                } );

                drainAndCount( clearService, "store: " + store.getKey() );
            }
        }
    }

    private Callable<Integer> clearPathProcessor( final String path, final StoreKey key, final Set<Group> groups )
    {
        try
        {
            ArtifactStore store = storeDataManager.getArtifactStore( key );
            return clearPathProcessor( path, store, groups );
        }
        catch ( IndyDataException e )
        {
            logger.error( "Cannot clear paths for missing / inaccessible store: " + key, e );
        }

        return ()->0;
    }

    private Callable<Integer> clearPathProcessor( final String path, final ArtifactStore store, final Set<Group> groups )
    {
        return () -> {
            logger.debug( "Got mergable transfer from diverged portion of membership: {}", path );

            int cleared = clearPath( path, store, groups, false );
            return cleared;
        };
    }

    private int clearPath( String path, ArtifactStore origin, Set<Group> affectedGroups, boolean deleteOriginPath )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        final boolean isReadonlyHosted = storeDataManager.isReadonly( origin );
        AtomicInteger cleared = new AtomicInteger( 0 );
        if ( deleteOriginPath && !isReadonlyHosted )
        {
            try
            {
                if ( delete( directContentAccess.getTransfer( origin, path ) ) )
                {
                    nfc.clearMissing( LocationUtils.toLocation( origin ) ); // clear NFC for this group
                    cleared.incrementAndGet();
                }
            }
            catch ( IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to retrieve transfer for: %s in origin store: %s. Reason: %s", path,
                                             origin.getKey(), e.getMessage() ), e );
            }
        }

        affectedGroups.forEach( g -> {
            try
            {
                Transfer gt = directContentAccess.getTransfer( g, path );
                if ( delete( gt ) )
                {
                    cleared.incrementAndGet();
                }
            }
            catch ( IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to retrieve transfer for: %s in group: %s. Reason: %s", path,
                                             g.getName(), e.getMessage() ), e );
            }
        } );

        logger.debug( "Clearing content via supplemental store-content actions..." );
        StreamSupport.stream( storeContentActions.spliterator(), false )
                     .forEach(
                             action -> action.clearStoreContent( path, origin, affectedGroups, deleteOriginPath ) );
        logger.debug( "All store-content actions done executing." );

        return cleared.get();
    }

    private boolean delete( Transfer t )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( t != null && t.exists() )
        {
            try
            {
                logger.debug( "Deleting: {}", t );
                boolean deleted = t.delete( true );
                if ( t.exists() )
                {
                    logger.error( "{} WAS NOT DELETED!", t );
                }

                return deleted;
            }
            catch ( IOException e )
            {
                logger.error( String.format( "Failed to delete: %s. Reason: %s", t, e.getMessage() ), e );
            }
        }

        return false;
    }

    private void listPathsAnd( StoreKey key, Predicate<? super String> pathFilter, Consumer<String> pathAction )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        Transfer root = null;
        try
        {
            root = directContentAccess.getTransfer( key, ROOT );

//            root.lockWrite();

            List<Transfer> toProcess = new ArrayList<>();
            toProcess.add( root );
            while ( !toProcess.isEmpty() )
            {
                Transfer next = toProcess.remove( 0 );
                try
                {
                    Stream.of(next.list()).forEach( filename->{
                        Transfer t = next.getChild( filename );
                        if ( t.isDirectory() )
                        {
                            logger.debug( "Adding directory path for processing: {}", t.getPath() );
                            toProcess.add( t );
                        }
                        else
                        {
                            logger.trace( "Testing file path: {}", t.getPath() );
                            if( pathFilter.test( t.getPath() ) )
                            {
                                logger.trace( "Adding file path to results: {}", t.getPath() );
                                pathAction.accept( t.getPath() );
                            }
                            else
                            {
                                logger.trace( "Skipping file path: {}", t.getPath() );
                            }
                        }
                    } );
                }
                catch ( IOException e )
                {
                    logger.error( String.format( "Failed to list contents of: %s. Reason: %s", next, e ), e );
                }
            }
        }
        catch ( IndyWorkflowException  e )
        {
            logger.error( String.format( "Failed to retrieve root directory reference for: %s. Reason: %s", key, e ), e );
        }
        finally
        {
            if ( root != null )
            {
                root.unlock();
            }
        }
    }

    /**
     * List the mergable paths in target store and clean up the paths in affected groups.
     * @param event
     * @param pathFilter
     * @param deleteOriginPath
     */
    private void processAllPaths( final IndyStoreEvent event, Predicate<? super String> pathFilter, boolean deleteOriginPath )
    {
        DrainingExecutorCompletionService<Integer> clearService =
                new DrainingExecutorCompletionService<>( cleanupExecutor );

        Set<StoreKey> keys = event.getStores().stream().map( store -> store.getKey() ).collect( Collectors.toSet() );
        keys.forEach(key->{
            final Set<Group> groups = new HashSet<>();
            try
            {
                groups.addAll(
                        storeDataManager.query().packageType( key.getPackageType() ).getGroupsAffectedBy( key ) );

                listPathsAnd( key, pathFilter, p->clearService.submit(clearPathProcessor( p, key, groups )) );
            }
            catch ( IndyDataException e )
            {
                logger.error( "Failed to retrieve groups affected by: " + key, e );
            }
        } );

        drainAndCount( clearService, "stores: " + keys );
    }

    private int drainAndCount( final DrainingExecutorCompletionService<Integer> clearService, final String description )
    {
        AtomicInteger count = new AtomicInteger( 0 );
        try
        {
            clearService.drain( partialCount -> count.addAndGet( partialCount ) );
        }
        catch ( InterruptedException | ExecutionException e )
        {
            logger.error( "Failed to clear paths related to change in " + description, e );
        }

        logger.debug( "Cleared {} paths for changes in {}", count.get(), description );

        return count.get();
    }

    /**
     * Extensive version for clean up paths. This will clean all mergable files in affected groups regardless whether
     * the path is in the target store cache or not. It also clears NFC.
     * @param event
     * @param pathFilter
     */
    private void processAllPathsExt( final IndyStoreEvent event, Predicate<? super String> pathFilter )
    {
        DrainingExecutorCompletionService<Integer> clearService =
                new DrainingExecutorCompletionService<>( cleanupExecutor );

        Set<ArtifactStore> stores = new HashSet<>( event.getStores() );
        stores.forEach(store->{
            final StoreKey key = store.getKey();
            try
            {
                Set<Group> groups =
                                storeDataManager.query().packageType( key.getPackageType() ).getGroupsAffectedBy( key );
                if ( store instanceof Group )
                {
                    groups.add( (Group) store );
                }

                groups.forEach( g->{
                    listPathsAnd( key, pathFilter, p->clearService.submit(clearPathProcessor( p, key, groups )) );
                } );

                nfc.clearMissing( LocationUtils.toLocation( store ) ); // clear NFC for this store
            }
            catch ( IndyDataException e )
            {
                e.printStackTrace();
            }
        } );

        Set<StoreKey> keys = event.getStores().stream().map( s -> s.getKey() ).collect( Collectors.toSet() );
        drainAndCount( clearService, "stores: " + keys );
    }

    private Predicate<? super String> mergablePathStrings()
    {
        return ( path ) -> {
            // here we care if it's mergable or not, since this may be triggered by a new file being stored.
            SpecialPathInfo specialPathInfo = specialPathManager.getSpecialPathInfo( path );
            return ( specialPathInfo != null && specialPathInfo.isMergable() );
        };
    }


}
