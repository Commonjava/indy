/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.MergedContentAction;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.core.content.group.GroupMergeHelper;
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
import org.infinispan.cdi.ConfigureCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.commonjava.indy.IndyContentConstants.CHECK_CACHE_ONLY;
import static org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger.METADATA_NAME;

/**
 * This listener will do these tasks:
 * <ul>
 *     <li>When there are member changes for a group, or member disabled/enabled in a group,
 *     delete group metadata caches to force regeneration of the metadata of the group (cascaded)</li>
 *     <li>When the metadata file changed or deleted of a member in a group,
 *     delete correspond cache of that file path of the member and group (cascaded)</li>
 * </ul>
 */
@ApplicationScoped
public class MetadataMergeListner
        implements MergedContentAction
{
    final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DirectContentAccess fileManager;

    @Inject
    private StoreDataManager storeManager;

    @ConfigureCache( "maven-version-metadata-cache" )
    @Inject
    @MavenVersionMetadataCache
    private CacheHandle<StoreKey, Map> versionMetadataCache;

    /**
     * Listen to an #{@link ArtifactStorePreUpdateEvent} and clear the metadata cache of the changed member in event
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
            Set<Group> affectedGroups = storeManager.query().getGroupsAffectedBy( store.getKey() );
            clearMergedPath( store, affectedGroups, path );
        }
        catch ( IndyDataException e )
        {
            logger.error( "Handle FileDeletionEvent failed", e );
        }
    }

    private void removeMetadataCache( ArtifactStore store )
    {
        versionMetadataCache.remove( store.getKey() );
        try
        {
            storeManager.query().getGroupsAffectedBy( store.getKey() ).forEach( g -> clearGroupMetaCache( g ) );
        }
        catch ( IndyDataException e )
        {
            logger.error( String.format( "Can not get affected groups of %s", store.getKey() ), e );
        }
    }

    private void removeMetadataCacheContent( final ArtifactStore store,
                                             final Map<ArtifactStore, ArtifactStore> changeMap )
    {
        handleStoreDisableOrEnable( store, changeMap );
        handleGroupMembersChanged( store, changeMap );
    }

    // if a store is disabled/enabled, we should clear its metadata cache and all of its affected groups cache too.
    private void handleStoreDisableOrEnable(final ArtifactStore store,
                                            final Map<ArtifactStore, ArtifactStore> changeMap){
        final ArtifactStore oldStore = changeMap.get( store );
        if ( store.isDisabled() != oldStore.isDisabled() )
        {
            removeMetadataCache( store );
        }
    }

    // If group members changed, should clear the cascading groups metadata cache
    private void handleGroupMembersChanged(final ArtifactStore store,
                                           final Map<ArtifactStore, ArtifactStore> changeMap){
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
                clearGroupMetaCache( group );
                try
                {
                    storeManager.query().getGroupsAffectedBy( group.getKey() ).forEach( g -> clearGroupMetaCache( g ) );
                }
                catch ( IndyDataException e )
                {
                    logger.error( String.format( "Can not get affected groups of %s", group.getKey()), e );
                }
            }
        }
    }

    private void clearGroupMetaCache( final Group group )
    {
        final Map<String, MetadataInfo> metadataMap = versionMetadataCache.get( group.getKey() );

        if ( metadataMap != null && !metadataMap.isEmpty() )
        {
            metadataMap.keySet().parallelStream().forEach( path -> {
                clearTempMetaFile( group, path );
            } );
        }

        versionMetadataCache.remove( group.getKey() );
    }

    private void clearTempMetaFile( final Group group, final String path )
    {
        try
        {
            final Transfer tempMetaTxfr = fileManager.getTransfer( group, path );
            if ( tempMetaTxfr != null && tempMetaTxfr.exists() )
            {
                tempMetaTxfr.delete();
            }
        }
        catch ( IndyWorkflowException | IOException e )
        {
            logger.error( "Can not delete temp metadata file for group. Group: {}, file path: {}", group, path );
        }

        try
        {
            final Transfer tempMetaMergeInfoTxfr =
                    fileManager.getTransfer( group, path + GroupMergeHelper.MERGEINFO_SUFFIX );
            if ( tempMetaMergeInfoTxfr != null && tempMetaMergeInfoTxfr.exists() )
            {
                tempMetaMergeInfoTxfr.delete();
            }
        }
        catch ( IndyWorkflowException | IOException e )
        {
            logger.error( "Can not delete temp metadata file for group. Group: {}, file path: {}", group, path );
        }
    }

    /**
     * Will clear the both merge path and merge info file of member and group contains that member(cascaded)
     * if that path of file changed in the member of #originatingStore
     */
    @Override
    public void clearMergedPath( ArtifactStore originatingStore, Set<Group> affectedGroups, String path )
    {
        final Map<String, MetadataInfo> metadataMap = versionMetadataCache.get( originatingStore.getKey() );

        if ( metadataMap != null && !metadataMap.isEmpty() )
        {
            if ( metadataMap.get( path ) != null )
            {
                metadataMap.remove( path );
                affectedGroups.forEach( group -> {
                    final Map<String, MetadataInfo> grpMetaMap = versionMetadataCache.get( group.getKey() );
                    if ( grpMetaMap != null && !grpMetaMap.isEmpty() )
                    {
                        clearTempMetaFile( group, path );
                        grpMetaMap.remove( path );
                    }
                } );
            }
        }
    }

}
