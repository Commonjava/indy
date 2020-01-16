package org.commonjava.indy.infinispan.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.infinispan.CacheHandle;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
