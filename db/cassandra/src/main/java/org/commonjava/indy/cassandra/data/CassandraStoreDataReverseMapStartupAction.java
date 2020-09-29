package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.data.StoreDataManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CassandraStoreDataReverseMapStartupAction
                implements StartupAction
{

    @Inject
    private StoreDataManager storeDataManager;

    @Override
    public void start() throws IndyLifecycleException
    {
        if ( storeDataManager instanceof CassandraStoreDataManager )
        {
            ( (CassandraStoreDataManager) storeDataManager ).initAffectedBy();
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
        return "Cassandra affected-by reverse-map initializer";
    }
}
