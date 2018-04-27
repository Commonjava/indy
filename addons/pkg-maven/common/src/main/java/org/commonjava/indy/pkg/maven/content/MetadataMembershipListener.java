package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.content.cache.MavenVersionMetadataCache;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MetadataMembershipListener
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private MavenMetadataGenerator metadataGenerator;

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
            final Map<String, MetadataInfo> metadataMap = versionMetadataCache.get( store.getKey() );
            if ( metadataMap != null && !metadataMap.isEmpty() )
            {
                versionMetadataCache.remove( store.getKey() );
//                try
//                {
//                    storeManager.query().getGroupsAffectedBy( store.getKey() ).forEach( g -> clearGroupMetaCache( g ) );
//                }
//                catch ( IndyDataException e )
//                {
//                    logger.error( String.format( "Can not get affected groups of %s", store.getKey() ), e );
//                }
            }
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
                clearGroupMetaCache( group );
                //                try
                //                {
                //                    storeManager.query().getGroupsAffectedBy( group.getKey() ).forEach( g -> clearGroupMetaCache( g ) );
                //                }
                //                catch ( IndyDataException e )
                //                {
                //                    logger.error( String.format( "Can not get affected groups of %s", group.getKey()), e );
                //                }
            }
            else
            {
                logger.trace( "No members changed, no need to expunge merged metadata" );
            }
        }
    }

    private void clearGroupMetaCache( final Group group )
    {
        final Map<String, MetadataInfo> metadataMap = versionMetadataCache.get( group.getKey() );
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Clearing metadata for group: {}\n{}", group.getKey(), metadataMap );

        if ( metadataMap != null && !metadataMap.isEmpty() )
        {
            String[] paths = new String[metadataMap.size()];
            paths = metadataMap.keySet().toArray( paths );

            metadataGenerator.clearAllMerged( group, paths );
        }

        versionMetadataCache.remove( group.getKey() );
    }

}
