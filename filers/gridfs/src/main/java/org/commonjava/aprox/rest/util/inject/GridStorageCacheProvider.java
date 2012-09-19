package org.commonjava.aprox.rest.util.inject;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.Cache;
import org.infinispan.io.GridFile;
import org.infinispan.manager.CacheContainer;

@Singleton
public class GridStorageCacheProvider
{
    @Inject
    private CacheContainer container;

    @Produces
    @AproxGridCache( AproxGridCaches.DATA )
    public Cache<String, byte[]> getDataCache()
    {
        return getCache( AproxGridCaches.DATA.cacheName() );
    }

    @Produces
    @AproxGridCache( AproxGridCaches.METADATA )
    public Cache<String, GridFile.Metadata> getMetadataCache()
    {
        return getCache( AproxGridCaches.METADATA.cacheName() );
    }

    public <K, V> Cache<K, V> getCache( final String name )
    {
        final Cache<K, V> cache = container.getCache( name );
        cache.start();

        return cache;
    }

}
