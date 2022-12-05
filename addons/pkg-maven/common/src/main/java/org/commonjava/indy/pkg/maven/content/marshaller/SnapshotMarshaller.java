/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class SnapshotMarshaller implements MessageMarshaller<Snapshot>
{
    @Override
    public Snapshot readFrom( ProtoStreamReader reader ) throws IOException
    {
        Snapshot snapshot = new Snapshot();
        snapshot.setTimestamp( reader.readString( "timestamp" ) );
        snapshot.setBuildNumber( reader.readInt( "buildNumber" ) );
        snapshot.setLocalCopy( reader.readBoolean( "localCopy" ) );
        return snapshot;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, Snapshot snapshot ) throws IOException
    {
        writer.writeString( "timestamp", snapshot.getTimestamp() );
        writer.writeInt( "buildNumber", snapshot.getBuildNumber());
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
        return "metadata_info.Snapshot";
    }
}
