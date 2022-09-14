/**
 * Copyright (C) 2022 Red Hat, Inc.
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

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.indy.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

/**
 * This Event Handler will listen to all StorePostUpdateEvent and StorePostDeleteEvent to update the
 * Cache of Store Data and Store Query Data which is used in ServiceStoreDataManager and ServiceStoreQuery
 *
 */
@ApplicationScoped
@SuppressWarnings( "unused" )
public class ServiceEventHandler
{
    @Inject
    private CacheProducer cacheProducer;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = "promote-validation-rules-executor", threads = 2 )
    private WeftExecutorService cacheUpdateExecutor;

    public void onStoreUpdate( @Observes ArtifactStorePostUpdateEvent updateEvent )
    {
        final BasicCacheHandle<StoreKey, ArtifactStore> storeCache =
                cacheProducer.getBasicCache( ServiceStoreDataManager.ARTIFACT_STORE );
        final BasicCacheHandle<Object, Collection<ArtifactStore>> queryCache =
                cacheProducer.getBasicCache( ServiceStoreQuery.ARTIFACT_STORE_QUERY );
        cacheUpdateExecutor.execute( () -> {
            for ( Map.Entry<ArtifactStore, ArtifactStore> storeEntry : updateEvent.getChangeMap().entrySet() )
            {
                final ArtifactStore newStore = storeEntry.getKey();
                final ArtifactStore originalStore = storeEntry.getValue();
                if ( storeCache.get( originalStore.getKey() ) != null )
                {
                    storeCache.remove( originalStore.getKey() );
                    storeCache.put( newStore.getKey(), newStore );
                }

                queryCache.execute( ( cache ) -> {
                    for ( Collection<ArtifactStore> stores : cache.values() )
                    {
                        for ( ArtifactStore store : stores )
                        {
                            if ( store.getKey().equals( originalStore.getKey() ) )
                            {
                                stores.remove( store );
                                stores.add( newStore );
                            }
                        }
                    }
                    return null;
                } );
            }
        } );
    }

    public void onStoreDelete( @Observes ArtifactStoreDeletePostEvent deleteEvent )
    {
        final BasicCacheHandle<StoreKey, ArtifactStore> storeCache =
                cacheProducer.getBasicCache( ServiceStoreDataManager.ARTIFACT_STORE );
        final BasicCacheHandle<Object, Collection<ArtifactStore>> queryCache =
                cacheProducer.getBasicCache( ServiceStoreQuery.ARTIFACT_STORE_QUERY );
        cacheUpdateExecutor.execute( () -> {
            for ( ArtifactStore deleted : deleteEvent.getStores() )
            {
                storeCache.execute( ( cache ) -> {
                    cache.remove( deleted.getKey() );
                    cache.values()
                         .stream()
                         .filter( ( store ) -> store.getType() == StoreType.group )
                         .forEach( ( store ) -> ( (Group) store ).getConstituents().remove( deleted.getKey() ) );
                    return null;
                } );

                queryCache.execute( ( cache ) -> {
                    cache.values().forEach( ( stores ) -> stores.forEach( ( store ) -> {
                        if ( store.getKey().equals( deleted.getKey() ) )
                        {
                            stores.remove( store );
                        }
                        else if ( store.getType() == StoreType.group )
                        {
                            ( (Group) store ).getConstituents().remove( deleted.getKey() );
                        }
                    } ) );
                    return null;
                } );
            }
        } );

    }
}
