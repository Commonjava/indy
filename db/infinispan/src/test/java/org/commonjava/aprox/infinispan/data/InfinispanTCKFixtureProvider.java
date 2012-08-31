package org.commonjava.aprox.infinispan.data;

import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;

public class InfinispanTCKFixtureProvider
    implements TCKFixtureProvider
{

    private InfinispanDataManager dataManager;

    @Override
    public synchronized StoreDataManager getDataManager()
    {
        if ( dataManager == null )
        {
            final CacheContainer cc = new DefaultCacheManager();
            final Cache<StoreKey, ArtifactStore> storeCache = cc.getCache( "stores" );
            dataManager = new InfinispanDataManager( storeCache );
        }

        return dataManager;
    }

}
