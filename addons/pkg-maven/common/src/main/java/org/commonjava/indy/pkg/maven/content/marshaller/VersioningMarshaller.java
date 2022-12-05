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
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.ArrayList;

public class VersioningMarshaller implements MessageMarshaller<Versioning>
{
    @Override
    public Versioning readFrom( ProtoStreamReader reader ) throws IOException
    {
        Versioning versioning = new Versioning();
        versioning.setLatest( reader.readString( "latest" ) );
        versioning.setRelease( reader.readString( "release" ) );
        versioning.setSnapshot( reader.readObject( "snapshot", Snapshot.class ) );
        versioning.setVersions( reader.readCollection( "versions", new ArrayList<String>(), String.class ) );
        versioning.setLastUpdated( reader.readString( "lastUpdated" ) );
        versioning.setSnapshotVersions( reader.readCollection( "snapshotVersions", new ArrayList<SnapshotVersion>(), SnapshotVersion.class ) );
        return versioning;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, Versioning versioning ) throws IOException
    {
        writer.writeString( "latest", versioning.getLatest() );
        writer.writeString( "release", versioning.getRelease() );
        writer.writeObject( "snapshot", versioning.getSnapshot(), Snapshot.class);
        writer.writeCollection( "versions", versioning.getVersions(), String.class );
        writer.writeString( "lastUpdated", versioning.getLastUpdated() );
        writer.writeCollection( "snapshotVersions", versioning.getSnapshotVersions(), SnapshotVersion.class );
    }

    @Override
    public Class<? extends Versioning> getJavaClass()
    {
        return Versioning.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_info.Versioning";
    }
}
