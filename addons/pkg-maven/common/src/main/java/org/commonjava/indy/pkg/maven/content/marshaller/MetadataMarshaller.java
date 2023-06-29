/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
        md.setVersioning( reader.readObject( "versioning", Versioning.class ) );
        md.setPlugins( reader.readCollection( "plugins", new ArrayList<Plugin>(), Plugin.class ) );
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
