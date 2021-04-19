package org.commonjava.indy.folo.action;

import org.commonjava.indy.core.conf.IndyDurableStateConfig;
import org.commonjava.indy.folo.data.FoloRecord;
import org.commonjava.indy.folo.data.FoloStoreToCassandra;
import org.commonjava.indy.folo.data.FoloStoretoInfinispan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.util.concurrent.atomic.AtomicInteger;

import static org.commonjava.indy.core.conf.IndyDurableStateConfig.STORAGE_CASSANDRA;

/**
 * This has problems when the dat is big and cluster will reboot indy if it takes too long.
 * I change it to call from MaintenanceHandler directly via REST.  Henry, 2021 Apr 19.
 */
public class FoloISPN2CassandraMigrationAction
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

    private boolean started;

    public boolean migrate()
    {
        if ( !STORAGE_CASSANDRA.equals( durableConfig.getFoloStorage() ) )
        {
            logger.info( "Skip the migration if the storage is not cassandra. " );
            return true;
        }
        if ( started )
        {
            logger.info( "Migration is already started. " );
            return true;
        }

        started = true;

        AtomicInteger count = new AtomicInteger( 0 );
        logger.info( "Migrate folo records from ISPN to cassandra, size: {}", cacheRecord.getSealedTrackingKey().size() );
        cacheRecord.getSealed().forEach( item -> {
            dbRecord.addSealedRecord( item );
            int index = count.incrementAndGet();
            if ( index % 10 == 0 )
            {
                logger.info( "{}", index ); // print some log to show the progress
            }
        } );
        logger.info( "Migrate folo records from ISPN to cassandra done." );
        return true;
    }
}
