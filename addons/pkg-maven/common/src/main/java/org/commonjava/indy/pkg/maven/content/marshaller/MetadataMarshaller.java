package org.commonjava.indy.pkg.maven.content.marshaller;

import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.infinispan.protostream.MessageMarshaller;
import org.apache.maven.artifact.repository.metadata.Metadata;

import java.io.IOException;
import java.util.ArrayList;

public class MetadataMarshaller implements MessageMarshaller<Metadata>
{
    @Override
    public Metadata readFrom( ProtoStreamReader reader ) throws IOException
    {
        Metadata md = new Metadata();
        md.setModelVersion( reader.readString( "modelVersion" ) );
        md.setGroupId( reader.readString( "groupId" ) );
        md.setArtifactId( reader.readString( "artifactId" ) );
        md.setVersion( reader.readString( "version" ) );
        md.setPlugins( reader.readCollection( "plugins", new ArrayList<Plugin>(), Plugin.class ) );
        md.setVersioning( reader.readObject( "versioning", Versioning.class ) );
        md.setModelEncoding( reader.readString( "modelEncoding" ) );
        return md;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, Metadata metadata ) throws IOException
    {
        writer.writeString( "modelVersion", metadata.getModelVersion() );
        writer.writeString( "groupId", metadata.getGroupId() );
        writer.writeString( "artifactId", metadata.getArtifactId() );
        writer.writeString( "version", metadata.getVersion() );
        writer.writeObject( "versioning", metadata.getVersioning(), Versioning.class );
        writer.writeCollection( "plugins", metadata.getPlugins(), Plugin.class );
        writer.writeString( "modelEncoding", metadata.getModelEncoding() );
    }

    @Override
    public Class<? extends Metadata> getJavaClass()
    {
        return Metadata.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_info.Metadata";
    }
}
