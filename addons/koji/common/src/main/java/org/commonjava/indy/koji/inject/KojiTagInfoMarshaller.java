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
package org.commonjava.indy.koji.inject;

import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.ArrayList;

public class KojiTagInfoMarshaller implements MessageMarshaller<KojiTagInfo>
{
    @Override
    public KojiTagInfo readFrom( ProtoStreamReader reader ) throws IOException
    {
        KojiTagInfo tagInfo = new KojiTagInfo();
        tagInfo.setId( reader.readInt( "id" ) );
        tagInfo.setName( reader.readString( "name" ) );
        tagInfo.setPermission( reader.readString( "permission" ) );
        tagInfo.setPermissionId( reader.readInt( "permissionId" ) );
        tagInfo.setArches( reader.readCollection( "arches", new ArrayList<>(), String.class ) );
        tagInfo.setLocked( reader.readBoolean( "locked" ) );
        tagInfo.setMavenSupport( reader.readBoolean( "mavenSupport" ) );
        tagInfo.setMavenIncludeAll( reader.readBoolean( "mavenIncludeAll" ) );
        return tagInfo;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, KojiTagInfo kojiTagInfo ) throws IOException
    {
        writer.writeInt( "id", kojiTagInfo.getId() );
        writer.writeString( "name", kojiTagInfo.getName() );
        writer.writeString( "permission", kojiTagInfo.getPermission() );
        writer.writeInt( "permissionId", kojiTagInfo.getPermissionId() );
        writer.writeCollection( "arches", kojiTagInfo.getArches(), String.class );
        writer.writeBoolean( "locked", kojiTagInfo.getLocked() );
        writer.writeBoolean( "mavenSupport", kojiTagInfo.getMavenSupport() );
        writer.writeBoolean( "mavenIncludeAll", kojiTagInfo.getMavenIncludeAll() );
    }

    @Override
    public Class<? extends KojiTagInfo> getJavaClass()
    {
        return KojiTagInfo.class;
    }

    @Override
    public String getTypeName()
    {
        return "koji.KojiTagInfo";
    }
}
