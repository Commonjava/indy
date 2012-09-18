package org.commonjava.aprox.infinispan.inject;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

public class AproxDataCacheProvider
{

    @Inject
    private CacheContainer container;

    @Produces
    @AproxCache( AproxCaches.DATA )
    public Cache<StoreKey, ArtifactStore> getDataCache()
    {
        return getCache( AproxCaches.DATA.cacheName() );
    }

    public <K, V> Cache<K, V> getCache( final String name )
    {
        final Cache<K, V> cache = container.getCache( name );
        cache.start();

        return cache;
    }

}
