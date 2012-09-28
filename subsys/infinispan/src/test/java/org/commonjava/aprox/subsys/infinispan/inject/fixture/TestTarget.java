package org.commonjava.aprox.subsys.infinispan.inject.fixture;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

@Singleton
public final class TestTarget
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

    public Cache<TestKey, TestValue> cache;

    public Cache<String, byte[]> dataCache;
}