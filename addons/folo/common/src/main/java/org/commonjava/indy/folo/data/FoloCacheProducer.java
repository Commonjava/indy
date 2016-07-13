package org.commonjava.indy.folo.data;

import org.commonjava.indy.subsys.infinispan.inject.qualifer.IndyCacheManager;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 */
public class FoloCacheProducer
{
    @Inject
    @IndyCacheManager
    private EmbeddedCacheManager cacheManager;

    @ConfigureCache( "folo-in-progress" )
    @FoloInprogressCache
    @Produces
    @ApplicationScoped
    public Configuration inProgressFoloRecordCacheCfg()
    {
        return cacheManager.getCacheConfiguration( "folo-in-progress" );
    }

    @ConfigureCache( "folo-sealed" )
    @FoloSealedCache
    @Produces
    @ApplicationScoped
    public Configuration sealedFoloRecordCacheCfg()
    {
        return cacheManager.getCacheConfiguration( "folo-sealed" );
    }
}
