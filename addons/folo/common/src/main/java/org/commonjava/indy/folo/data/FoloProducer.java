package org.commonjava.indy.folo.data;

import org.commonjava.indy.core.conf.IndyDurableStateConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;


@ApplicationScoped
public class FoloProducer {

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );
    
    @Inject
    IndyDurableStateConfig durableStateConfig;


    @Produces
    @ApplicationScoped
    public FoloRecord getFoloRecordCassandra(@FoloStoreToCassandra FoloRecord dbRecord
            ,@FoloStoretoInfinispan FoloRecord cacheRecord) {
        return IndyDurableStateConfig.STORAGE_CASSANDRA.equals( durableStateConfig.getFoloStorage() ) ? dbRecord : cacheRecord;
    }


}
