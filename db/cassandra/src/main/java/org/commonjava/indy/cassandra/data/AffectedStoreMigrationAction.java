package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.data.StoreDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@Named( "cassandra-affected-store-data-migration" )
public class AffectedStoreMigrationAction implements MigrationAction
{

    @Inject
    StoreDataManager dataManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public boolean migrate() throws IndyLifecycleException
    {
        if ( dataManager instanceof CassandraStoreDataManager )
        {
            if ( !((CassandraStoreDataManager)dataManager).isAffectedEmpty() )
            {
                logger.info( "Affected store is not empty. Migration already done." );
                return true;
            }

            logger.info( "Init affected stores based on the store data" );

            ( (CassandraStoreDataManager) dataManager ).initAffectedBy();
        }

        return true;
    }

    @Override
    public int getMigrationPriority()
    {
        return 99;
    }

    @Override
    public String getId()
    {
        return "Init affected store table based on the original store data.";
    }
}
