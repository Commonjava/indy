package org.commonjava.indy.folo.data;

import org.commonjava.indy.folo.conf.FoloConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;


@ApplicationScoped
public class FoloProducer {

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    FoloConfig foloConfig;


    @Produces
    @ApplicationScoped
    public FoloRecord getFoloRecordCassandra(@FoloStoreToCassandra FoloRecord dbRecord
            ,@FoloStoretoInfinispan FoloRecord cacheRecord) {
        return foloConfig.getStoreToCassandra() ? dbRecord : cacheRecord;
    }


}
