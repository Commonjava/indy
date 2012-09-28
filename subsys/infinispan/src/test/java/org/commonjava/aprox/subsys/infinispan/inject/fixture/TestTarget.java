package org.commonjava.aprox.subsys.infinispan.inject.fixture;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.infinispan.Cache;
import org.infinispan.cdi.ConfigureCache;

@Singleton
public final class TestTarget
{
    @Inject
    @ConfigureCache( "test" )
    public Cache<TestKey, TestValue> cache;

    @Inject
    @ConfigureCache( "testData" )
    public Cache<String, byte[]> dataCache;
}