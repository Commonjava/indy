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
        version.setVersion( reader.readString( "version" ) );
        version.setUpdated( reader.readString( "updated" ) );
        return version;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, SnapshotVersion snapshotVersion ) throws IOException
    {
        writer.writeString( "classifier", snapshotVersion.getClassifier() );
        writer.writeString( "extension", snapshotVersion.getExtension() );
        writer.writeString( "version", snapshotVersion.getVersion() );
        writer.writeString( "updated", snapshotVersion.getUpdated() );
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
