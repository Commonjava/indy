/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.implrepo.change;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.indy.change.event.IndyLifecycleEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.implrepo.ImpliedReposException;
import org.commonjava.indy.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.indy.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.atlas.maven.ident.util.JoinString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpliedRepoMaintainer
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private ImpliedRepoMetadataManager metadataManager;

    @Inject
    private ImpliedRepoConfig config;

    protected ImpliedRepoMaintainer()
    {
    }

    public ImpliedRepoMaintainer( final StoreDataManager storeManager, final ImpliedRepoMetadataManager metadataManager,
                                  final ImpliedRepoConfig config )
    {
        this.storeManager = storeManager;
        this.metadataManager = metadataManager;
        this.config = config;
    }

    public void scanAtStart( @Observes final IndyLifecycleEvent event )
    {
        if ( event.getType() != IndyLifecycleEvent.Type.started )
        {
            return;
        }

        logger.info( "Scanning for unincorporated repository implications." );
        try
        {
            final Map<StoreKey, ArtifactStore> stores = mapStores( storeManager.getAllArtifactStores() );

            if ( stores != null )
            {
                for ( final ArtifactStore store : stores.values() )
                {
                    processStore( store, stores );
                }
            }
        }
        catch ( final IndyDataException e )
        {
            logger.error( "Failed to retrieve all known stores.", e );
        }
        catch ( Throwable error )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( String.format( "Implied-repository maintenance failed: %s", error.getMessage() ), error );
        }

    }

    // FIXME: How to tell whether a repo that is implied by other repos was added manually??? 
    // That, vs. just left there after the repo that implied it was removed???
    // We cannot currently remove formerly implied repos because we can't distinguish between the above states.
    public void updateImpliedStores( @Observes final ArtifactStorePreUpdateEvent event )
    {
        if ( !storeManager.isStarted() )
        {
            return;
        }

        if ( !config.isEnabled() )
        {
            logger.debug( "Implied-repository processing is not enabled." );
            return;
        }

        try
        {
            // TODO: Update for changes map.
            final Map<StoreKey, ArtifactStore> currentStores = mapStores( event );

            for ( final ArtifactStore store : event )
            {
                logger.debug( "Processing store: {} for implied repo metadata", store );
                processStore( store, currentStores );
            }
        }
        catch ( Throwable error )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( String.format( "Implied-repository maintenance failed: %s", error.getMessage() ), error );
        }
    }

    private void processStore( final ArtifactStore store, final Map<StoreKey, ArtifactStore> currentStores )
    {
        final ImpliedRepoMaintJob job = new ImpliedRepoMaintJob( store, currentStores );
        if ( !initJob( job ) )
        {
            return;
        }

        if ( processImpliedRepos( job ) )
        {
            logger.info( "Group: {} updated with {} implied repositories.", job.group.getKey(), job.added.size() );

            // Since we're getting in ahead of persistence, we shouldn't need to store anything in a store that was in the event.
            //                final String message =
            //                    String.format( "On update of group: %s, implied membership was recalculated.\n\nAdded:"
            //                        + "\n  %s\n\nNOTE: This update may have resulted in stores that were previously "
            //                        + "implied by another member persisting as members even after the store that implied "
            //                        + "them was removed.", store.getName(), new JoinString( "\n  ", job.added ) );
            //
            //                final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, message );
            //                try
            //                {
            //                    storeManager.storeArtifactStore( job.store, summary );
            //                }
            //                catch ( final IndyDataException e )
            //                {
            //                    logger.error( "Failed to store implied-membership changes to: " + store.getKey(), e );
            //                }
        }
    }

    private Map<StoreKey, ArtifactStore> mapStores( final Iterable<ArtifactStore> stores )
    {
        final Map<StoreKey, ArtifactStore> result = new HashMap<>();
        if ( stores != null )
        {
            for ( final ArtifactStore store : stores )
            {
                result.put( store.getKey(), store );
            }
        }

        return result;
    }

    private boolean processImpliedRepos( final ImpliedRepoMaintJob job )
    {
        final Set<StoreKey> processed = new HashSet<>();

        // pre-load all existing reachable members to processed list, to prevent re-adding them via 
        // implications. Reachable means they could be in nested groups.
        for ( final ArtifactStore member : job.reachableMembers )
        {
            processed.add( member.getKey() );
        }

        logger.debug( "Preset processed-implications to reachable members:\n  {}",
                      new JoinString( "\n  ", processed ) );

        int lastLen = 0;
        boolean changed = false;
        job.added = new ArrayList<>();

//        job.group = job.group.copyOf();

        // iterate through group membership looking for implied stores that aren't already members.
        // For each implied store:
        //  1. load the store
        //  2. add the implied store's key to the processed list
        //  3. add the implied store's key to the group's membership
        //  4. add the implied store to the members list
        // As soon as we go an iteration without adding a new member, we've captured everything.
        do
        {
            lastLen = job.members.size();

            for ( final ArtifactStore member : new ArrayList<>( job.members ) )
            {
                logger.debug( "Processing member: {} for implied repos within group: {}", member.getKey(),
                              job.group.getKey() );
                processed.add( member.getKey() );
                List<StoreKey> implied;
                try
                {
                    implied = metadataManager.getStoresImpliedBy( member );
                }
                catch ( final ImpliedReposException e )
                {
                    logger.error( "Failed to retrieve implied-store metadata for: " + member.getKey(), e );
                    continue;
                }

                if ( implied == null || implied.isEmpty() )
                {
                    continue;
                }

                implied.removeAll( processed );

                for ( final StoreKey key : implied )
                {
                    logger.debug( "Found implied store: {} not already in group: {}", key, job.group.getKey() );
                    ArtifactStore impliedStore;
                    try
                    {
                        impliedStore = storeManager.getArtifactStore( key );
                    }
                    catch ( final IndyDataException e )
                    {
                        logger.error( "Failed to retrieve store: " + key + " implied by: " + member.getKey(), e );
                        continue;
                    }

                    logger.info( "Adding: {} to group: {} (implied by POMs in: {})", key, job.group.getKey(),
                                 member.getKey() );

                    processed.add( key );
                    job.added.add( key );

                    job.group.addConstituent( key );

                    job.members.add( impliedStore );
                    changed = true;
                }
            }
        }
        while ( job.members.size() > lastLen );

        return changed;
    }

    private boolean initJob( final ImpliedRepoMaintJob job )
    {
        // TODO: Regardless of whether it's a group, look for implied repo metadata. If found, find the
        // groups this store belongs to and update them. If we use a pre/post event pair like with delete,
        // we might be able to fix the repo-removal problem too.
        if ( !( job.store instanceof Group ) )
        {
            logger.debug( "ImpliedRepoMaint: Ignoring non-group: {}", job.store.getKey() );
            return false;
        }

        if ( !config.isEnabledForGroup( job.store.getName() ) )
        {
            logger.debug( "ImpliedRepoMaint: Implied repositories not enabled for group: {}", job.store.getKey() );
            return false;
        }

        logger.debug( "Processing group: {} for stores implied by membership which are not yet in the membership",
                      job.store.getName() );
        job.group = (Group) job.store;

        try
        {
            // getOrderedStoresInGroup(), but we can't use persisted info...
            job.members = loadMemberStores( job.group, job );

            // getOrderedConcreteStores(), but we can't use the persisted info...
            final LinkedHashSet<ArtifactStore> reachable = new LinkedHashSet<>( job.members.size() );
            for ( final ArtifactStore member : job.members )
            {
                if ( member instanceof Group )
                {
                    reachable.addAll( loadMemberStores( (Group) member, job ) );
                }
                else
                {
                    reachable.add( member );
                }
            }

            job.reachableMembers = new ArrayList<>( reachable );

            logger.debug( "For group: {}\n Members: {}\n  Reachable Concrete Members: {}", job.group.getKey(),
                          job.members, job.reachableMembers );
        }
        catch ( final IndyDataException e )
        {
            logger.error( "Failed to retrieve member stores for group: " + job.group.getName(), e );
        }

        if ( job.members == null )
        {
            logger.debug( "ImpliedRepoMaint: Group: {} has no membership", job.store.getKey() );
            return false;
        }

        return true;
    }

    private List<ArtifactStore> loadMemberStores( final Group group, final ImpliedRepoMaintJob job )
        throws IndyDataException
    {
        final List<StoreKey> constituents = new ArrayList<>( group.getConstituents() );
        final List<ArtifactStore> members = new ArrayList<>( constituents.size() );

        for ( final StoreKey memberKey : constituents )
        {
            ArtifactStore store = job.currentStores.get( memberKey );
            if ( store == null )
            {
                store = storeManager.getArtifactStore( memberKey );
            }

            if ( store == null )
            {
                logger.warn( "Store not found for key: {} (member of: {})", memberKey, group.getKey() );
                continue;
            }

            members.add( store );
        }

        return members;
    }

    public static final class ImpliedRepoMaintJob
    {
        List<StoreKey> added;

        Group group;

        List<ArtifactStore> reachableMembers;

        List<ArtifactStore> members;

        final ArtifactStore store;

        final Map<StoreKey, ArtifactStore> currentStores;

        public ImpliedRepoMaintJob( final ArtifactStore store, final Map<StoreKey, ArtifactStore> currentStores )
        {
            this.store = store;
            this.currentStores = currentStores;
        }
    }

}
