/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.db.service;

import org.commonjava.cdi.util.weft.NamedThreadFactory;
import org.commonjava.indy.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.indy.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.commons.api.BasicCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.commonjava.indy.change.event.ArtifactStoreUpdateType.ADD;

/**
 * This class will listen to all StorePostUpdateEvent and StorePostDeleteEvent to update the
 * Cache of Store Data and Store Query Data which is used in ServiceStoreDataManager and ServiceStoreQuery
 *
 */
@ApplicationScoped
@SuppressWarnings( "unused" )
public class ServiceStoreDataCacheUpdater
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CacheProducer cacheProducer;

    //TODO: we found a bug of weft with o11yphant TraceManager, which could cause ConcurrentModificationException.
    //      Before fixing it here will use a JUC ExecutorService instead.
    //      The exception is something like:
    //          java.util.ConcurrentModificationException
    //            at java.base/java.util.HashMap.forEach(HashMap.java:1339)
    //            at java.base/java.util.Collections$UnmodifiableMap.forEach(Collections.java:1505)
    //            at org.commonjava.o11yphant.trace.TraceManager.lambda$startThreadRootSpan$1(TraceManager.java:117)
    private final ExecutorService cacheUpdateExecutor = Executors.newFixedThreadPool( 2, new NamedThreadFactory(
            "service-data-cache-update-executor", new ThreadGroup( "service-data-cache-update-executor" ), true, 3 ) );

    public void onStoreUpdate( @Observes ArtifactStorePostUpdateEvent updateEvent )
    {
        logger.info( "Start cache updater for store post update event: {}", updateEvent );
        final BasicCacheHandle<StoreKey, ArtifactStore> storeCache =
                cacheProducer.getBasicCache( ServiceStoreDataManager.ARTIFACT_STORE );
        cacheUpdateExecutor.execute( () -> {
            if ( updateEvent.getType().equals( ADD ) )
            {
                for ( ArtifactStore newStore : updateEvent.getChanges() )
                {
                    logger.info( "Fresh the store cache on add event, newStore:{}, disabled:{}", newStore,
                                 newStore.isDisabled() );
                    storeCache.put( newStore.getKey(), newStore );
                    obsoleteQueryCache( newStore.getKey() );
                }
            }
            else
            {
                for ( Map.Entry<ArtifactStore, ArtifactStore> storeEntry : updateEvent.getChangeMap().entrySet() )
                {

                    final ArtifactStore newStore = storeEntry.getKey();
                    final ArtifactStore originalStore = storeEntry.getValue();
                    if ( storeCache.get( originalStore.getKey() ) != null )
                    {
                        logger.info(
                                "Fresh the store cache on update event, originalStore:{}, disabled:{}, newStore:{}, disabled:{}",
                                originalStore, originalStore.isDisabled(), newStore, newStore.isDisabled() );
                        storeCache.remove( originalStore.getKey() );
                        storeCache.put( newStore.getKey(), newStore );
                    }

                    obsoleteQueryCache( newStore.getKey() );
                }
            }
        } );
    }

    public void onStoreDelete( @Observes ArtifactStoreDeletePostEvent deleteEvent )
    {
        logger.info( "Start cache updater for store delete post event: {}", deleteEvent );
        final BasicCacheHandle<StoreKey, ArtifactStore> storeCache =
                cacheProducer.getBasicCache( ServiceStoreDataManager.ARTIFACT_STORE );
        cacheUpdateExecutor.execute( () -> {
            for ( ArtifactStore deleted : deleteEvent.getStores() )
            {
                storeCache.execute( ( cache ) -> {
                    logger.info( "Fresh store cache on delete event, deleted: {}", deleted );
                    cache.remove( deleted.getKey() );
                    cache.values()
                         .stream()
                         .filter( ( store ) -> store.getType() == StoreType.group )
                         .forEach( ( store ) -> {
                             List<StoreKey> storeList = ( (Group) store ).getConstituents();
                             List<StoreKey> stores = new ArrayList<>( storeList );
                             stores.remove( deleted.getKey() );
                             ( (Group) store ).setConstituents( stores );
                         } );
                    return null;
                } );

                obsoleteQueryCache( deleted.getKey() );
            }
        } );

    }

    /**
     * For ServiceStoreQuery cache, we need to clear the related entry if any store event happened and
     * let the query refresh from remote repository service
     *
     * @param storeKey
     */
    private void obsoleteQueryCache( StoreKey storeKey )
    {
        final BasicCacheHandle<Object, Collection<ArtifactStore>> queryCache =
                cacheProducer.getBasicCache( ServiceStoreQuery.ARTIFACT_STORE_QUERY );
        queryCache.execute( ( cache ) -> {
            Collection<ArtifactStore> affectedGroups = new HashSet<>();
            for ( Map.Entry<Object, Collection<ArtifactStore>> entry : cache.entrySet() )
            {
                Object key = entry.getKey();
                // This is for getGroupsAffectedBy query cache
                if ( key instanceof Set && ( (Set) key ).contains( storeKey ) )
                {
                    logger.info( "Fresh the store query cache, removed: {}", storeKey );
                    affectedGroups.addAll( cache.get( key ) );
                    cache.remove( key );
                }
                // This is for getOrderedConcreteStoresInGroup query cache
                clearOrderedConcreteStoresCache( key, storeKey, cache );
            }

            // This is for affectedGroups' getOrderedConcreteStoresInGroup query cache
            for ( ArtifactStore group : affectedGroups )
            {
                logger.info( "Fresh the store query cache for affectedGroups, removed: {}", storeKey );
                for ( Map.Entry<Object, Collection<ArtifactStore>> entry : cache.entrySet() )
                {
                    Object key = entry.getKey();
                    clearOrderedConcreteStoresCache( key, group.getKey(), cache );

                }
            }
            return null;
        } );
    }

    private void clearOrderedConcreteStoresCache( Object key, StoreKey storeKey,
                                                  BasicCache<Object, Collection<ArtifactStore>> cache )
    {
        final boolean isConcreteStoreCache =
                storeKey.getType() == StoreType.group && key instanceof String && ( (String) key ).contains(
                        String.format( "%s:%s", storeKey.getPackageType(), storeKey.getName() ) );
        if ( isConcreteStoreCache )
        {
            logger.info( "Fresh the concrete stores query cache, removed: {}", storeKey );
            cache.remove( key );
        }
    }
}
