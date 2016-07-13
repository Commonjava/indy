package org.commonjava.indy.content.index;

import org.commonjava.indy.subsys.infinispan.inject.qualifer.IndyCacheManager;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class ContentIndexCacheProducer
{
    @Inject
    @IndyCacheManager
    private EmbeddedCacheManager cacheManager;

    @ConfigureCache( "content-index" )
    @ContentIndexCache
    @Produces
    @ApplicationScoped
    public Configuration contentIndexCacheCfg()
    {
        return cacheManager.getCacheConfiguration( "content-index" );
    }
}
