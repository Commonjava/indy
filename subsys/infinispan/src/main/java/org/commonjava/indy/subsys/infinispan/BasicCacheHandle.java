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
package org.commonjava.indy.subsys.infinispan;

import com.codahale.metrics.Timer;
import org.commonjava.indy.metrics.IndyMetricsManager;
import org.infinispan.commons.api.BasicCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static org.commonjava.indy.metrics.IndyMetricsConstants.METER;
import static org.commonjava.indy.metrics.IndyMetricsConstants.TIMER;

public class BasicCacheHandle<K,V>
{
    Logger logger = LoggerFactory.getLogger( getClass() );

    private String name;

    protected BasicCache<K,V> cache;

    private IndyMetricsManager metricsManager;

    private String metricPrefix;

    private boolean stopped;

    public boolean isStopped()
    {
        return stopped;
    }

    protected BasicCacheHandle()
    {
    }

    protected BasicCacheHandle( String named, BasicCache<K, V> cache, IndyMetricsManager metricsManager, String metricPrefix )
    {
        this.name = named;
        this.cache = cache;
        this.metricsManager = metricsManager;
        this.metricPrefix = metricPrefix;
    }

    public BasicCacheHandle( String named, BasicCache<K, V> cache )
    {
        this( named, cache, null, null );
    }

    public BasicCache<K,V> getCache(){
        return cache;
    }

    public String getName()
    {
        return name;
    }

    public <R> R execute( Function<BasicCache<K, V>, R> operation )
    {
        if ( !stopped )
        {
            Timer.Context context = startMetrics( "execute" );

            try
            {
                return operation.apply( cache );
            }
            catch ( RuntimeException e )
            {
                logger.error( "Failed to complete operation: " + e.getMessage(), e );
            }
            finally
            {
                if ( context != null )
                {
                    context.stop();
                }
            }
        }
        else
        {
            logger.error( "Cannot complete operation. Cache {} is shutting down.", name );
        }

        return null;
    }

    public void stop()
    {
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

    public V put( K key, V value, int expiration, TimeUnit timeUnit )
    {
        return execute( cache -> cache.put( key, value, expiration, timeUnit ) );
    }

    public V putIfAbsent( K key, V value )
    {
        return execute( ( c ) -> c.putIfAbsent( key, value ) );
    }

    public V computeIfAbsent( K key, Function<? super K, ? extends V> mappingFunction )
    {
        return execute( c -> c.computeIfAbsent( key, mappingFunction ) );
    }

    public V remove( K key )
    {
        return execute( cache -> cache.remove( key ) );
    }

    public V get( K key )
    {
        return execute( cache -> cache.get( key ) );
    }

    protected Timer.Context startMetrics( String name )
    {
        if ( metricsManager != null )
        {
            metricsManager.getMeter( name( metricPrefix, name, METER ) ).mark();
            return metricsManager.getTimer( name( metricPrefix, name, TIMER ) ).time();
        }

        return null;
    }

    public Set<K> cacheKeySetByFilter( Predicate<K> filter )
    {
        return this.cache.keySet().stream().filter( filter ).collect( Collectors.toSet() );
    }

    public boolean isEmpty(){
        return execute( c->c.isEmpty() );
    }

}
