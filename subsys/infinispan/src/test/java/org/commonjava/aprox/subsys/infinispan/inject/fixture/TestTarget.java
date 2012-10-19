package org.commonjava.aprox.subsys.infinispan.inject.fixture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

@javax.enterprise.context.ApplicationScoped
public class TestTarget
{
    @Inject
    private CacheContainer container;

    @PostConstruct
    public void injectCaches()
    {
        cache = container.getCache( "test" );
        cache.start();

        dataCache = container.getCache( "testData" );
        dataCache.start();
    }

    private Cache<TestKey, TestValue> cache;

    private Cache<String, byte[]> dataCache;

    public Cache<TestKey, TestValue> getCache()
    {
        return cache;
    }

    public Cache<String, byte[]> getDataCache()
    {
        return dataCache;
    }
}