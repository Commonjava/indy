package org.commonjava.indy.pkg.maven.content.marshaller;

import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class SnapshotMarshaller implements MessageMarshaller<Snapshot>
{
    @Override
    public Snapshot readFrom( ProtoStreamReader reader ) throws IOException
    {
        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber( reader.readInt( "buildNumber" ) );
        snapshot.setTimestamp( reader.readString( "timestamp" ) );
        snapshot.setLocalCopy( reader.readBoolean( "localCopy" ) );
        return snapshot;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, Snapshot snapshot ) throws IOException
    {
        writer.writeInt( "buildNumber", snapshot.getBuildNumber());
        writer.writeString( "timestamp", snapshot.getTimestamp() );
        writer.writeBoolean( "localCopy", snapshot.isLocalCopy());
    }

    @Override
    public Class<? extends Snapshot> getJavaClass()
    {
        return Snapshot.class;
    }

    @Override
    public String getTypeName()
    {
        return "maven.Snapshot";
    }
}
