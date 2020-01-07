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
package org.commonjava.indy.infinispan.data;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.NoOpStoreEventDispatcher;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.db.common.AbstractStoreDataManager;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.commonjava.indy.infinispan.data.StoreDataCacheProducer.STORE_BY_PKG_CACHE;
import static org.commonjava.indy.infinispan.data.StoreDataCacheProducer.STORE_DATA_CACHE;

@ApplicationScoped
@Alternative
public class InfinispanStoreDataManager
                extends AbstractStoreDataManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @StoreDataCache
    private CacheHandle<StoreKey, ArtifactStore> stores;

    @Inject
    @StoreByPkgCache
    private CacheHandle<String, Map<StoreType, Set<StoreKey>>> storesByPkg;

    @Inject
    private StoreEventDispatcher dispatcher;


    @Override
    protected StoreEventDispatcher getStoreEventDispatcher()
    {
        return dispatcher;
    }

    protected InfinispanStoreDataManager()
    {
    }

    @PostConstruct
    synchronized void init()
    {
        // re-fill the stores by package cache each time when reboot
        if ( storesByPkg != null )
        {
            logger.info( "Clean the stores-by-pkg cache" );
            storesByPkg.clear();
        }
        final Set<ArtifactStore> allStores = getAllArtifactStores();
        logger.info( "There are {} stores need to fill in stores-by-pkg cache", allStores.size() );
        for ( ArtifactStore store : allStores )
        {
            final Map<StoreType, Set<StoreKey>> typedKeys =
                    storesByPkg.computeIfAbsent( store.getKey().getPackageType(), k -> new HashMap<>() );
            final Set<StoreKey> keys = typedKeys.computeIfAbsent( store.getKey().getType(), k -> new HashSet<>() );
            keys.add( store.getKey() );
        }
    }

    public InfinispanStoreDataManager( final Cache<StoreKey, ArtifactStore> cache,
                                       final Cache<String, Map<StoreType, Set<StoreKey>>> storesByPkg )
    {
        this.dispatcher = new NoOpStoreEventDispatcher();
        this.stores = new CacheHandle( STORE_DATA_CACHE, cache );
        this.storesByPkg = new CacheHandle( STORE_BY_PKG_CACHE, storesByPkg );
    }

    @Override
    protected ArtifactStore getArtifactStoreInternal( StoreKey key )
    {
        return stores.get( key );
    }

    @Override
    protected synchronized ArtifactStore removeArtifactStoreInternal( StoreKey key )
    {
        final ArtifactStore store = stores.remove( key );
        final Map<StoreType, Set<StoreKey>> typedKeys = storesByPkg.get( key.getPackageType() );
        if ( typedKeys != null )
        {
            final Set<StoreKey> keys = typedKeys.get( key.getType() );
            if ( keys != null )
            {
                keys.remove( key );
            }
        }
        return store;
    }

    @Override
    public void clear( final ChangeSummary summary )
    {
        stores.clear();
        storesByPkg.clear();
    }

    @Override
    @Measure
    public Set<ArtifactStore> getAllArtifactStores()
    {
        return stores.executeCache( c -> new HashSet<>( c.values() ), "getAllStores" );
    }

    @Override
    @Measure
    public Map<StoreKey, ArtifactStore> getArtifactStoresByKey()
    {
        return stores.executeCache( c -> {
            Map<StoreKey, ArtifactStore> ret = new HashMap<>();
            c.values().forEach( v -> ret.put( v.getKey(), v ) );
            return ret;

        }, "getAllStoresByKey" );
    }

    @Override
    public boolean hasArtifactStore( final StoreKey key )
    {
        return stores.containsKey( key );
    }

    @Override
    public boolean isStarted()
    {
        return true;
    }

    @Override
    public boolean isEmpty()
    {
        return stores.isEmpty();
    }

    @Override
    @Measure
    public Stream<StoreKey> streamArtifactStoreKeys()
    {
        return stores.executeCache( c->c.keySet().stream() );
    }

    @Override
    protected synchronized ArtifactStore putArtifactStoreInternal( StoreKey storeKey, ArtifactStore store )
    {
        final ArtifactStore added = stores.put( storeKey, store );
        final Map<StoreType, Set<StoreKey>> typedKeys =
                storesByPkg.computeIfAbsent( storeKey.getPackageType(), k -> new HashMap<>() );
        final Set<StoreKey> keys = typedKeys.computeIfAbsent( storeKey.getType(), k -> new HashSet<>() );
        keys.add( storeKey );
        return added;
    }

    @Override
    public Set<StoreKey> getStoreKeysByPkg( final String pkg )
    {
        final Map<StoreType, Set<StoreKey>> typedKeys = storesByPkg.get( pkg );
        if ( typedKeys != null )
        {
            final Set<StoreKey> keys = new HashSet<>();
            typedKeys.values().forEach( keys::addAll );
            logger.trace( "There are {} stores for package type {}", keys.size(), pkg );
            return keys;
        }
        else
        {
            logger.trace( "There is no store for package type {}", pkg );
            return Collections.emptySet();
        }
    }

    @Override
    public Set<StoreKey> getStoreKeysByPkgAndType( final String pkg, final StoreType type )
    {
        final Map<StoreType, Set<StoreKey>> typedKeys = storesByPkg.get( pkg );
        if ( typedKeys != null )
        {
            final Set<StoreKey> keys = typedKeys.get( type );
            if ( keys != null )
            {
                logger.trace( "There are {} stores for package type {} with type {}", keys.size(), pkg, type );
                return new HashSet<>( keys );
            }
        }
        logger.trace( "There is no store for package type {} with type {}", pkg, type );
        return Collections.emptySet();
    }

}
