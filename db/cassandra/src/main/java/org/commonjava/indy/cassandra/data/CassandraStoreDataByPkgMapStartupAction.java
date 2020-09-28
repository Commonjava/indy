package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.data.StoreDataManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CassandraStoreDataByPkgMapStartupAction implements StartupAction
{

    @Inject
    StoreDataManager storeDataManager;

    @Override
    public void start() throws IndyLifecycleException
    {
        if ( storeDataManager instanceof CassandraStoreDataManager )
        {
            ( (CassandraStoreDataManager) storeDataManager ).initByPkgMap();
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
        return "Cassandra by-pkg map initializer";
    }
}
