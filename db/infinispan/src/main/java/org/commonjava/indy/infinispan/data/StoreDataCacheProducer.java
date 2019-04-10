package org.commonjava.indy.infinispan.data;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class StoreDataCacheProducer
{
    public static final String STORE_DATA_CACHE = "store-data";

    @Inject
    private CacheProducer cacheProducer;

    @StoreDataCache
    @Produces
    @ApplicationScoped
    public CacheHandle<StoreKey, String> getStoreDataCache()
    {
        return cacheProducer.getCache( STORE_DATA_CACHE );
    }

}