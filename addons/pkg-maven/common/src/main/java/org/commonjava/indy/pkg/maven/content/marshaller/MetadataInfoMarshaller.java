package org.commonjava.indy.pkg.maven.content.marshaller;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.commonjava.indy.pkg.maven.content.MetadataInfo;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class MetadataInfoMarshaller implements MessageMarshaller<MetadataInfo>
{
    @Override
    public MetadataInfo readFrom( ProtoStreamReader reader ) throws IOException
    {
        MetadataInfo info = new MetadataInfo( reader.readObject( "metadata", Metadata.class) );
        info.setMetadataMergeInfo( reader.readString( "metadataMergeInfo" ) );
        return info;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, MetadataInfo metadataInfo ) throws IOException
    {
        writer.writeString( "metadataMergeInfo", metadataInfo.getMetadataMergeInfo() );
        writer.writeObject( "metadata", metadataInfo.getMetadata(), Metadata.class );
    }

    @Override
    public Class<? extends MetadataInfo> getJavaClass()
    {
        return MetadataInfo.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_info.MetadataInfo";
    }
}
