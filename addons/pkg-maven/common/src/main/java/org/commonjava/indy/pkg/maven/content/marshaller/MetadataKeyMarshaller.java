package org.commonjava.indy.pkg.maven.content.marshaller;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.MetadataKey;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class MetadataKeyMarshaller implements MessageMarshaller<MetadataKey>
{
    @Override
    public MetadataKey readFrom( ProtoStreamReader reader ) throws IOException
    {
        StoreKey storeKey = reader.readObject( "storeKey", StoreKey.class );
        String path = reader.readString( "path" );
        return new MetadataKey( storeKey, path );
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, MetadataKey metadataKey ) throws IOException
    {
        writer.writeString( "path", metadataKey.getPath() );
        writer.writeObject( "storeKey", metadataKey.getStoreKey(), StoreKey.class);
    }

    @Override
    public Class<? extends MetadataKey> getJavaClass()
    {
        return MetadataKey.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_key.MetadataKey";
    }
}
