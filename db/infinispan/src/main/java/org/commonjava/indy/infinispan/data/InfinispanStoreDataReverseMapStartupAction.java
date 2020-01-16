package org.commonjava.indy.infinispan.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class InfinispanStoreDataReverseMapStartupAction
        implements StartupAction
{
    @Inject
    private StoreDataManager storeDataManager;

    @Override
    public void start()
    {
        if ( storeDataManager instanceof InfinispanStoreDataManager )
        {
            ( (InfinispanStoreDataManager) storeDataManager ).initAffectedBy();
        }
    }

    @Override
    public int getStartupPriority()
    {
        return 10;
    }

    @Override
    public String getId()
    {
        return "Infinispan affected-by reverse-map initializer";
    }
}
