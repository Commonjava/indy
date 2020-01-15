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
            throws IndyLifecycleException
    {
        try
        {
            ((InfinispanStoreDataManager)storeDataManager).initAffectedBy();
        }
        catch ( IndyDataException e )
        {
            throw new IndyLifecycleException( "Failed to reverse-map stores and groups they affect: " + e.getMessage(),
                                              e );
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
