package org.commonjava.indy.pkg.maven.content.marshaller;

import org.apache.maven.artifact.repository.metadata.Plugin;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class PluginMarshaller implements MessageMarshaller<Plugin>
{
    @Override
    public Plugin readFrom( ProtoStreamReader reader ) throws IOException
    {
        Plugin plugin = new Plugin();
        plugin.setArtifactId( reader.readString( "artifactId" ) );
        plugin.setName( reader.readString( "name" ) );
        plugin.setPrefix( reader.readString( "prefix" ) );
        return plugin;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, Plugin plugin ) throws IOException
    {
        writer.writeString( "artifactId", plugin.getArtifactId());
        writer.writeString( "name", plugin.getName() );
        writer.writeString( "prefix", plugin.getPrefix());
    }

    @Override
    public Class<? extends Plugin> getJavaClass()
    {
        return Plugin.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_info.Plugin";
    }
}
