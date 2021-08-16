package org.commonjava.indy.db.common.marshaller;

import org.commonjava.indy.db.common.cache.StoreKeySet;
import org.commonjava.indy.model.core.StoreKey;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.HashSet;

public class StoreKeySetMarshaller implements MessageMarshaller<StoreKeySet>
{
    @Override
    public StoreKeySet readFrom( ProtoStreamReader reader ) throws IOException
    {
        StoreKeySet storeKeySet = new StoreKeySet();
        storeKeySet.setStoreKeys( reader.readCollection( "storeKeys", new HashSet<StoreKey>(), StoreKey.class ) );
        return storeKeySet;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, StoreKeySet storeKeySet ) throws IOException
    {
        writer.writeCollection("storeKeys", storeKeySet.getStoreKeys(), StoreKey.class);
    }

    @Override
    public Class<? extends StoreKeySet> getJavaClass()
    {
        return StoreKeySet.class;
    }

    @Override
    public String getTypeName()
    {
        return "affected_stores.StoreKeySet";
    }
}
