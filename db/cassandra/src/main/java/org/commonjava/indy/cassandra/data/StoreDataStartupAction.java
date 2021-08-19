package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.data.StoreDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named("Store-Cache-Initialization")
public class StoreDataStartupAction implements StartupAction
{

    @Inject
    StoreDataManager dataManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public String getId()
    {
        return "Init remote store cache based on the persistent store data.";
    }

    @Override
    public void start() throws IndyLifecycleException
    {

        if ( dataManager instanceof CassandraStoreDataManager )
        {
            logger.info( "Init the cache of remote stores based on the store data" );

            ( (CassandraStoreDataManager) dataManager ).initRemoteStoresCache();
        }

    }

    @Override
    public int getStartupPriority()
    {
        return 90;
    }
}
