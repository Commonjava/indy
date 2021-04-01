package org.commonjava.indy.folo.action;

import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.core.conf.IndyDurableStateConfig;
import org.commonjava.indy.folo.data.FoloRecord;
import org.commonjava.indy.folo.data.FoloStoreToCassandra;
import org.commonjava.indy.folo.data.FoloStoretoInfinispan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static org.commonjava.indy.core.conf.IndyDurableStateConfig.STORAGE_CASSANDRA;

public class FoloISPN2CassandraMigrationAction
                implements MigrationAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @FoloStoreToCassandra
    FoloRecord dbRecord;

    @Inject
    @FoloStoretoInfinispan
    FoloRecord cacheRecord;

    @Inject
    IndyDurableStateConfig durableConfig;

    @Override
    public boolean migrate()
    {
        if ( !STORAGE_CASSANDRA.equals( durableConfig.getFoloStorage() ) )
        {
            logger.info( "Skip the migration if the storage is not cassandra. " );
            return true;
        }

        return doMigrate();
    }

    private boolean doMigrate()
    {
        logger.info( "Migrate folo records from ISPN to cassandra, size: {}", cacheRecord.getSealedTrackingKey().size() );
        cacheRecord.getSealed().forEach( item -> dbRecord.addSealedRecord( item ) );
        logger.info( "Migrate folo records from ISPN to cassandra done." );
        return true;
    }

    @Override
    public int getMigrationPriority()
    {
        return 90;
    }

    @Override
    public String getId()
    {
        return "folo migration from infinispan to cassandra";
    }
}
