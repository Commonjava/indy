package org.commonjava.indy.db.common.cache;

import org.commonjava.indy.db.common.marshaller.StoreKeyMarshaller;
import org.commonjava.indy.db.common.marshaller.StoreKeySetMarshaller;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.subsys.infinispan.config.ISPNRemoteConfiguration;
import org.infinispan.protostream.BaseMarshaller;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class CommonStoreDataCacheProducer
{

    public static final String AFFECTED_BY_STORE_CACHE = "affected-by-stores";

    @Inject
    private CacheProducer cacheProducer;

    @Inject
    private ISPNRemoteConfiguration remoteConfiguration;

    @AffectedByStoreCache
    @Produces
    @ApplicationScoped
    public BasicCacheHandle<StoreKey, StoreKeySet> getAffectedByStores()
    {

        if ( remoteConfiguration.isEnabled() )
        {
            List<BaseMarshaller> keysMarshallers = new ArrayList<>();
            keysMarshallers.add( new StoreKeyMarshaller());
            keysMarshallers.add( new StoreKeySetMarshaller());
            cacheProducer.registerProtoAndMarshallers( "affected_store.proto", keysMarshallers );

        }
        return cacheProducer.getBasicCache( AFFECTED_BY_STORE_CACHE );
    }

}
