package org.commonjava.indy.content.index;

import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.subsys.infinispan.inject.qualifer.IndyCache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class ContentIndexCacheProducer
{
    @Inject
    private CacheProducer cacheProducer;

    @ContentIndexCache
    @Produces
    @ApplicationScoped
    public CacheHandle<IndexedStorePath, IndexedStorePath> contentIndexCacheCfg()
    {
        return cacheProducer.getCache( "content-index", IndexedStorePath.class, IndexedStorePath.class );
    }
}
