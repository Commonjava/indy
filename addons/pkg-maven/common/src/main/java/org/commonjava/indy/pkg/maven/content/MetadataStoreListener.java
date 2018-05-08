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
package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.pkg.maven.content.cache.MavenVersionMetadataCache;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.commonjava.indy.IndyContentConstants.CHECK_CACHE_ONLY;
import static org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger.METADATA_NAME;

/**
 * This listener will do these tasks:
 * <ul>
 *     <li>When there are member changes for a group, or some members disabled/enabled in a group, delete group metadata caches to force next regeneration of the metadata files of the group(cascaded)</li>
 * </ul>
 */
@ApplicationScoped
public class MetadataStoreListener
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MavenMetadataGenerator metadataGenerator;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    @MavenVersionMetadataCache
    private CacheHandle<StoreKey, Map> versionMetadataCache;

    /**
     * Listen to an #{@link ArtifactStorePreUpdateEvent} and clear the metadata cache due to changed memeber in that event
     *
     * @param event
     */
    public void onStoreUpdate( @Observes final ArtifactStorePreUpdateEvent event )
    {
        logger.trace( "Got store-update event: {}", event );

        if ( ArtifactStoreUpdateType.UPDATE == event.getType() )
        {
            for ( ArtifactStore store : event )
            {
                removeMetadataCacheContent( store, event.getChangeMap() );
            }
        }
    }

    public void onStoreDelete( @Observes final ArtifactStoreDeletePreEvent event )
    {
        logger.trace( "Got store-delete event: {}", event );

        for ( ArtifactStore store : event )
        {
            removeMetadataCache( store );
        }
    }

    /**
     * Indy normally does not handle FileDeletionEvent when the cached metadata files were deleted due to store
     * enable/disable/delete, etc. Lately we add a force-deletion for group/remote-repo cache files. This requires to
     * delete affected group metadata files and clear ISPN cache too. We use this method for just this case, i.e.,
     * only if CHECK_CACHE_ONLY is true.
     */
    public void onMetadataFileForceDelete( @Observes final FileDeletionEvent event )
    {
        EventMetadata eventMetadata = event.getEventMetadata();
        if ( !Boolean.TRUE.equals( eventMetadata.get( CHECK_CACHE_ONLY ) ) )
        {
            return;
        }

        logger.trace( "Got file-delete event: {}", event );

        Transfer transfer = event.getTransfer();
        String path = transfer.getPath();

        if ( !path.endsWith( METADATA_NAME ) )
        {
            logger.trace( "Not {} , path: {}", METADATA_NAME, path );
            return;
        }

        Location loc = transfer.getLocation();

        if ( !( loc instanceof KeyedLocation ) )
        {
            logger.trace( "Ignore FileDeletionEvent, not a KeyedLocation, location: {}", loc );
            return;
        }

        KeyedLocation keyedLocation = (KeyedLocation) loc;
        StoreKey storeKey = keyedLocation.getKey();
        try
        {
            ArtifactStore store = storeManager.getArtifactStore( storeKey );
            metadataGenerator.clearAllMerged( store, path );
        }
        catch ( IndyDataException e )
        {
            logger.error( "Handle FileDeletionEvent failed", e );
        }
    }

    private void removeMetadataCacheContent( final ArtifactStore store,
                                             final Map<ArtifactStore, ArtifactStore> changeMap )
    {
        logger.trace( "Processing update event for: {}", store.getKey() );
        handleStoreDisableOrEnable( store, changeMap );

        handleGroupMembersChanged( store, changeMap );
    }

    // if a store is disabled/enabled, we should clear its metadata cache and all of its affected groups cache too.
    private void handleStoreDisableOrEnable(final ArtifactStore store,
                                            final Map<ArtifactStore, ArtifactStore> changeMap){
        logger.trace( "Processing en/disable event for: {}", store.getKey() );

        final ArtifactStore oldStore = changeMap.get( store );
        if ( store.isDisabled() != oldStore.isDisabled() )
        {
            logger.trace( "En/disable state changed for: {}", store.getKey() );
            removeMetadataCache( store );
        }
        else
        {
            logger.trace( "En/disable state has not changed for: {}", store.getKey() );
        }
    }

    // If group members changed, should clear the cascading groups metadata cache
    private void handleGroupMembersChanged(final ArtifactStore store,
                                           final Map<ArtifactStore, ArtifactStore> changeMap)
    {
        final StoreKey key = store.getKey();
        if ( StoreType.group == key.getType() )
        {
            final List<StoreKey> newMembers = ( (Group) store ).getConstituents();
            logger.trace( "New members of: {} are: {}", store.getKey(), newMembers );

            final Group group = (Group) changeMap.get( store );
            final List<StoreKey> oldMembers = group.getConstituents();
            logger.trace( "Old members of: {} are: {}", group.getName(), oldMembers );

            boolean membersChanged = false;

            if ( newMembers.size() != oldMembers.size() )
            {
                membersChanged = true;
            }
            else
            {
                for ( StoreKey storeKey : newMembers )
                {
                    if ( !oldMembers.contains( storeKey ) )
                    {
                        membersChanged = true;
                    }
                }
            }

            if ( membersChanged )
            {
                logger.trace( "Membership change confirmed. Clearing caches for group: {} and groups affected by it.", group.getKey() );
                clearGroupMetaCache( group, group );
                try
                {
                    storeManager.query().getGroupsAffectedBy( group.getKey() ).forEach( g -> clearGroupMetaCache( g, group ) );
                }
                catch ( IndyDataException e )
                {
                    logger.error( String.format( "Can not get affected groups of %s", group.getKey() ), e );
                }
            }
            else
            {
                logger.trace( "No members changed, no need to expunge merged metadata" );
            }
        }
    }

    private void removeMetadataCache( ArtifactStore store )
    {
        logger.trace( "Removing cached metadata for: {}", store.getKey() );

        versionMetadataCache.remove( store.getKey() );
        try
        {
            storeManager.query().getGroupsAffectedBy( store.getKey() ).forEach( g -> clearGroupMetaCache( g, store ) );
        }
        catch ( IndyDataException e )
        {
            logger.error( String.format( "Can not get affected groups of %s", store.getKey() ), e );
        }
    }

    private void clearGroupMetaCache( final Group group, final ArtifactStore store )
    {
        final Map<String, MetadataInfo> metadataMap = versionMetadataCache.get( group.getKey() );

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Clearing metadata for group: {} on store update: {}\n{}", group.getKey(), store.getKey(), metadataMap );

        if ( metadataMap == null || metadataMap.isEmpty() )
        {
            logger.trace( "No cached metadata for: {}", group.getKey() );
            return;
        }

        String[] paths = new String[metadataMap.size()];
        paths = metadataMap.keySet().toArray( paths );

        List<String> pathsList = Arrays.asList( paths );

        logger.trace(
                "Clearing merged paths in MavenMetadataGenerator for: {} as a result of change in: {} (paths: {})",
                group.getKey(), store.getKey(), pathsList );

        metadataGenerator.clearAllMerged( group, paths );

        logger.trace( "Clearing cached, merged paths for: {} as a result of change in: {} (paths: {})", group.getKey(),
                      store.getKey(), pathsList );

        versionMetadataCache.remove( group.getKey() );
    }

}
