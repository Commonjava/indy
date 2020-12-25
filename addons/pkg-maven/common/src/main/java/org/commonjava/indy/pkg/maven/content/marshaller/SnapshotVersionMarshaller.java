package org.commonjava.indy.pkg.maven.content.marshaller;

import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class SnapshotVersionMarshaller implements MessageMarshaller<SnapshotVersion>
{
    @Override
    public SnapshotVersion readFrom( ProtoStreamReader reader ) throws IOException
    {
        SnapshotVersion version = new SnapshotVersion();
        version.setClassifier( reader.readString( "classifier" ) );
        version.setExtension( reader.readString( "extension" ) );
        version.setUpdated( reader.readString( "updated" ) );
        version.setVersion( reader.readString( "version" ) );
        return version;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, SnapshotVersion snapshotVersion ) throws IOException
    {
        writer.writeString( "classifier", snapshotVersion.getClassifier() );
        writer.writeString( "extension", snapshotVersion.getExtension() );
        writer.writeString( "updated", snapshotVersion.getUpdated() );
        writer.writeString( "version", snapshotVersion.getVersion() );
    }

    @Override
    public Class<? extends SnapshotVersion> getJavaClass()
    {
        return SnapshotVersion.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_info.SnapshotVersion";
    }
}
