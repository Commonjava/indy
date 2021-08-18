package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class StoreDataCacheProducer
{

    public static final String REMOTE_KOJI_STORE = "remote-koji-stores";

    @Inject
    private CacheProducer cacheProducer;

    @RemoteKojiStoreDataCache
    @Produces
    @ApplicationScoped
    public CacheHandle<StoreKey, ArtifactStore> getRemoteKojiStoreDataCache()
    {
        return cacheProducer.getCache(REMOTE_KOJI_STORE);
    }

}
