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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.NoOpStoreEventDispatcher;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.db.common.AbstractStoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.commonjava.indy.infinispan.data.StoreDataCacheProducer.STORE_DATA_CACHE;

@ApplicationScoped
@Alternative
public class InfinispanStoreDataManager
                extends AbstractStoreDataManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @StoreDataCache
    private CacheHandle<StoreKey, String> stores;

    //TODO: Warning: this in mem facade store cache may bring mem issue if the repo grows to very big number in future!
    private final Map<StoreKey, ArtifactStore> inMemoryStores = new ConcurrentHashMap<>();

    @Inject
    private CacheProducer cacheProducer;

    @Inject
    private StoreEventDispatcher dispatcher;

    @Inject
    private IndyObjectMapper serializer;

    @Override
    protected StoreEventDispatcher getStoreEventDispatcher()
    {
        return dispatcher;
    }

    protected InfinispanStoreDataManager()
    {
    }

    @PostConstruct
    private void init()
    {
    }

    public InfinispanStoreDataManager( final Cache<String, String> jsonStoreDataCache,
                                       final IndyObjectMapper serializer )
    {
        this.dispatcher = new NoOpStoreEventDispatcher();
        this.stores = new CacheHandle( STORE_DATA_CACHE, jsonStoreDataCache );
        this.serializer = serializer;
    }

    @Override
    protected ArtifactStore getArtifactStoreInternal( StoreKey key )
    {
        return inMemoryStores.computeIfAbsent( key, k -> readValueByJson( stores.get( k ), k ) );
    }

    private ArtifactStore readValueByJson( String json, StoreKey key )
    {
        if ( json == null )
        {
            return null;
        }
        try
        {
            return serializer.readValue( json, key.getType().getStoreClass() );
        }
        catch ( IOException e )
        {
            logger.error( "Failed to read value", e );
        }
        return null;
    }

    @Override
    protected ArtifactStore removeArtifactStoreInternal( StoreKey key )
    {
        ArtifactStore removed;
        String json;
        synchronized ( inMemoryStores )
        {
            removed = inMemoryStores.remove( key );
            json = stores.executeCache( ( c ) -> c.remove( key ) );
        }
        if ( removed == null )
        {
            removed = readValueByJson( json, key );
        }
        return removed;
    }

    @Override
    public void clear( final ChangeSummary summary ) throws IndyDataException
    {
        inMemoryStores.clear();
        stores.executeCache( c -> {
            c.clear();
            return null;
        } );
    }

    @Override
    public Set<ArtifactStore> getAllArtifactStores()
            throws IndyDataException
    {
        fillInMemoryStores();
        return new HashSet<>( inMemoryStores.values() );
    }

    private void fillInMemoryStores()
    {
        if ( inMemoryStores.size() != stores.executeCache( Cache::size ) )
        {
            logger.info( "In memory stores mismatch with ispn stores, will re-fill it now.." );
            stores.executeCache( c -> {
                c.forEach( ( k, v ) -> {
                    if ( !inMemoryStores.containsKey( k ) )
                    {
                        ArtifactStore store = readValueByJson( v, k );
                        if ( store == null )
                        {
                            // I think a null store is not valid, so remove it here.
                            c.remove( k );
                        }
                        else
                        {
                            putInMemoryStores( store );
                        }
                    }
                } );
                return null;
            } );
        }
    }

    @Override
    public Map<StoreKey, ArtifactStore> getArtifactStoresByKey()
    {
        fillInMemoryStores();
        return Collections.unmodifiableMap( inMemoryStores );
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
    protected ArtifactStore putArtifactStoreInternal( StoreKey storeKey, ArtifactStore store )
    {
        final String json;
        try
        {
            json = serializer.writeValueAsString( store );
        }
        catch ( JsonProcessingException e )
        {
            logger.error( "Failed to put", e );
            return null;
        }

        synchronized ( inMemoryStores )
        {
            String org = stores.put( storeKey, json );
            putInMemoryStores( readValueByJson( json, storeKey ) );
        }
        return inMemoryStores.get( storeKey );
    }

    private void putInMemoryStores( final ArtifactStore store )
    {
        // ConcurrentHashMap does not allow null key and null value
        if ( store != null )
        {
            inMemoryStores.put( store.getKey(), store );
        }
    }
}
