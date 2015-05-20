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
package org.commonjava.aprox.implrepo.change;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.implrepo.ImpliedReposException;
import org.commonjava.aprox.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.aprox.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.maven.atlas.ident.util.JoinString;
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

    public ImpliedRepoMaintainer( final StoreDataManager storeManager,
                                  final ImpliedRepoMetadataManager metadataManager, final ImpliedRepoConfig config )
    {
        this.storeManager = storeManager;
        this.metadataManager = metadataManager;
        this.config = config;
    }

    // FIXME: How to tell whether a repo that is implied by other repos was added manually??? 
    // That, vs. just left there after the repo that implied it was removed???
    // We cannot currently remove formerly implied repos because we can't distinguish between the above states.
    public void updateImpliedStores( @Observes final ArtifactStoreUpdateEvent event )
    {
        if ( !config.isEnabled() )
        {
            logger.debug( "Implied-repository processing is not enabled." );
            return;
        }

        for ( final ArtifactStore store : event )
        {
            if ( !( store instanceof Group ) )
            {
                continue;
            }

            final Group group = (Group) store;

            List<ArtifactStore> members = null;
            List<ArtifactStore> reachableMembers = null;
            try
            {
                members = storeManager.getOrderedStoresInGroup( store.getName() );
                reachableMembers = storeManager.getOrderedConcreteStoresInGroup( store.getName() );
            }
            catch ( final AproxDataException e )
            {
                logger.error( "Failed to retrieve member stores for group: " + store.getName(), e );
            }

            if ( members == null )
            {
                continue;
            }

            final Set<StoreKey> processed = new HashSet<>();

            // pre-load all existing reachable members to processed list, to prevent re-adding them via 
            // implications. Reachable means they could be in nested groups.
            for ( final ArtifactStore member : reachableMembers )
            {
                processed.add( member.getKey() );
            }

            int lastLen = 0;
            boolean changed = false;
            final List<StoreKey> added = new ArrayList<>();

            // iterate through group membership looking for implied stores that aren't already members.
            // For each implied store:
            //  1. load the store
            //  2. add the implied store's key to the processed list
            //  3. add the implied store's key to the group's membership
            //  4. add the implied store to the members list
            // As soon as we go an iteration without adding a new member, we've captured everything.
            do
            {
                lastLen = members.size();

                for ( final ArtifactStore member : members )
                {
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
                        if ( processed.contains( key ) )
                        {
                            continue;
                        }

                        ArtifactStore impliedStore;
                        try
                        {
                            impliedStore = storeManager.getArtifactStore( key );
                        }
                        catch ( final AproxDataException e )
                        {
                            logger.error( "Failed to retrieve store: " + key + " implied by: " + member.getKey(), e );
                            continue;
                        }

                        processed.add( key );
                        added.add( key );
                        group.addConstituent( key );
                        members.add( impliedStore );
                        changed = true;
                    }
                }
            }
            while ( members.size() > lastLen );

            if ( changed )
            {
                final String message =
                    String.format( "On update of group: %s, implied membership was recalculated.\n\nAdded:"
                        + "\n  %s\n\nNOTE: This update may have resulted in stores that were previously "
                        + "implied by another member persisting as members even after the store that implied "
                        + "them was removed.", store.getName(), new JoinString( "\n  ", added ) );

                final ChangeSummary summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, message );
                try
                {
                    storeManager.storeGroup( group, summary );
                }
                catch ( final AproxDataException e )
                {
                    logger.error( "Failed to store implied-membership changes to: " + store.getKey(), e );
                }
            }
        }
    }

}
