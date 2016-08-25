package org.commonjava.indy.subsys.infinispan;

import org.infinispan.Cache;
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

    public String getName()
    {
        return name;
    }

    public <R> R execute( Function<Cache<K, V>, R> operation )
    {
        if ( !stopped )
        {
            return operation.apply( cache );
        }
        else
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Cannot complete operation. Cache {} is shutting down.", name );
            return null;
        }
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
