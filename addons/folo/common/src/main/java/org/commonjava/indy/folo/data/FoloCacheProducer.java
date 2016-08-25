package org.commonjava.indy.folo.data;

import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.subsys.infinispan.inject.qualifer.IndyCache;
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
    private CacheProducer cacheProducer;

    @FoloInprogressCache
    @Produces
    @ApplicationScoped
    public CacheHandle<TrackedContentEntry, TrackedContentEntry> inProgressFoloRecordCacheCfg()
    {
        return cacheProducer.getCache( "folo-in-progress", TrackedContentEntry.class, TrackedContentEntry.class );
    }

    @FoloSealedCache
    @Produces
    @ApplicationScoped
    public CacheHandle<TrackingKey, TrackedContent> sealedFoloRecordCacheCfg()
    {
        return cacheProducer.getCache( "folo-sealed", TrackingKey.class, TrackedContent.class );
    }
}
