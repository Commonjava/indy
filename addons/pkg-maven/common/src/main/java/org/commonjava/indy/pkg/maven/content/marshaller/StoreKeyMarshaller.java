package org.commonjava.indy.pkg.maven.content.marshaller;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class StoreKeyMarshaller implements MessageMarshaller<StoreKey>
{
    @Override
    public StoreKey readFrom( ProtoStreamReader reader ) throws IOException
    {
        String packageType = reader.readString( "packageType" );
        StoreType storeType = reader.readEnum( "type", StoreType.class );
        String name = reader.readString( "name" );
        return new StoreKey( packageType, storeType, name );
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, StoreKey storeKey ) throws IOException
    {
        writer.writeString( "name", storeKey.getName() );
        writer.writeString( "packageType", storeKey.getPackageType() );
        writer.writeEnum( "type", storeKey.getType());
    }

    @Override
    public Class<? extends StoreKey> getJavaClass()
    {
        return StoreKey.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_key.StoreKey";
    }
}
