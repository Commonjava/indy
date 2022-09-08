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
        writer.writeObject( "metadata", metadataInfo.getMetadata(), Metadata.class );
        writer.writeString( "metadataMergeInfo", metadataInfo.getMetadataMergeInfo() );
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
