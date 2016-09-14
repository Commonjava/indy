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
package org.commonjava.indy.subsys.infinispan;

import org.infinispan.Cache;
import org.infinispan.IllegalLifecycleStateException;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Holder class that helps manage the shutdown process for things that use Infinispan.
 */
public class CacheHandle<K,V>
{
    private String name;

    private Cache<K,V> cache;

    private boolean stopped;

    protected CacheHandle(){}

    public CacheHandle( String named, Cache<K, V> cache )
    {
        this.name = named;
        this.cache = cache;
    }

    //FIXME: as new CacheProvider construction mechanism used a original cache to construct FastLocalCacheProvider,
    //       here we need to expose the wrapped cache out. Need to think some alternative way later to fix this.
    @Deprecated
    public Cache<K,V> getCache(){
        return cache;
    }

    public String getName()
    {
        return name;
    }

    public <R> R execute( Function<Cache<K, V>, R> operation )
    {
        if ( !stopped )
        {
            try
            {
                return operation.apply( cache );
            }
            catch ( RuntimeException e )
            {
                // this may happen if the cache is in the process of shutting down
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( "Failed to complete operation: " + e.getMessage(), e );
            }
        }
        else
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Cannot complete operation. Cache {} is shutting down.", name );
        }

        return null;
    }

    public void stop()
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Cache {} is shutting down!", name );
        this.stopped = true;
    }

    public boolean containsKey( K key )
    {
        return execute( cache -> cache.containsKey( key ) );
    }

    public V put( K key, V value )
    {
        return execute( cache -> cache.put( key, value ) );
    }

    public V remove( K key )
    {
        return execute( cache -> cache.remove( key ) );
    }

    public V get( K key )
    {
        return execute( cache -> cache.get( key ) );
    }
}
