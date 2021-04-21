package org.commonjava.indy.folo.action;

import org.commonjava.indy.core.conf.IndyDurableStateConfig;
import org.commonjava.indy.folo.data.FoloRecord;
import org.commonjava.indy.folo.data.FoloStoreToCassandra;
import org.commonjava.indy.folo.data.FoloStoretoInfinispan;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackingKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.util.Set;
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

        logger.info( "Migrate folo records from ISPN to cassandra start" );
        started = true;

        AtomicInteger count = new AtomicInteger( 0 );
        Set<TrackingKey> keySet = cacheRecord.getSealedTrackingKey();

        logger.info( "Get folo records size: {}", keySet.size() );
        keySet.forEach( key -> {
            TrackedContent item = cacheRecord.get( key );
            dbRecord.addSealedRecord( item );
            int index = count.incrementAndGet();
            if ( index % 10 == 0 )
            {
                logger.info( "{}", index ); // print some log to show the progress
            }
        } );
        logger.info( "{}", count.get() );
/*
 * This can not work if the entries are too many. It will hang Indy.
 *
        cacheRecord.getSealed().forEach( item -> {
            dbRecord.addSealedRecord( item );
            int index = count.incrementAndGet();
            if ( index % 10 == 0 )
            {
                logger.info( "{}", index ); // print some log to show the progress
            }
        } );
*/
        logger.info( "Migrate folo records from ISPN to cassandra done." );
        return true;
    }
}
