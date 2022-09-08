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

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class StoreKeyMarshaller implements MessageMarshaller<StoreKey>
{
    @Override
    public StoreKey readFrom( ProtoStreamReader reader ) throws IOException
    {
        String packageType = reader.readString( "packageType" );
        StoreType storeType = reader.readEnum( "type", StoreType.class );
        String name = reader.readString( "name" );
        return new StoreKey( packageType, storeType, name );
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, StoreKey storeKey ) throws IOException
    {
        writer.writeString( "packageType", storeKey.getPackageType() );
        writer.writeEnum( "type", storeKey.getType());
        writer.writeString( "name", storeKey.getName() );
    }

    @Override
    public Class<? extends StoreKey> getJavaClass()
    {
        return StoreKey.class;
    }

    @Override
    public String getTypeName()
    {
        return "metadata_key.StoreKey";
    }
}
