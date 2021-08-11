/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
