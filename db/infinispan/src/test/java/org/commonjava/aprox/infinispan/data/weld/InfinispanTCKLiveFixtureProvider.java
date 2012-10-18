package org.commonjava.aprox.infinispan.data.weld;

import javax.inject.Inject;

import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.infinispan.data.InfinispanDataManager;

@javax.enterprise.context.ApplicationScoped
public class InfinispanTCKLiveFixtureProvider
    implements TCKFixtureProvider
{

    @Inject
    private InfinispanDataManager dataManager;

    @Override
    public synchronized StoreDataManager getDataManager()
    {
        return dataManager;
    }

}
