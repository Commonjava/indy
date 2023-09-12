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

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.MetadataKey;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class MetadataKeyMarshaller implements MessageMarshaller<MetadataKey>
{
    @Override
    public MetadataKey readFrom( ProtoStreamReader reader ) throws IOException
    {
        StoreKey storeKey = reader.readObject( "storeKey", StoreKey.class );
        String path = reader.readString( "path" );
        return new MetadataKey( storeKey, path );
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, MetadataKey metadataKey ) throws IOException
    {
        writer.writeObject( "storeKey", metadataKey.getStoreKey(), StoreKey.class);
        writer.writeString( "path", metadataKey.getPath() );
    }

    @Override
    public Class<? extends MetadataKey> getJavaClass()
    {
        return MetadataKey.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_key.MetadataKey";
    }
}
