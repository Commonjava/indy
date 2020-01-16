package org.commonjava.indy.infinispan.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.data.StoreDataManager;

import javax.inject.Inject;

public class InfinispanStoreDataByPkgMapStartupAction
        implements StartupAction
{

    @Inject
    private StoreDataManager storeDataManager;

    @Override
    public void start()
            throws IndyLifecycleException
    {
        if ( storeDataManager instanceof InfinispanStoreDataManager )
        {
            ((InfinispanStoreDataManager)storeDataManager).initByPkgMap();
        }
    }

    @Override
    public int getStartupPriority()
    {
        return 11;
    }

    @Override
    public String getId()
    {
        return "Infinispan by-pkg map initializer";
    }
}
