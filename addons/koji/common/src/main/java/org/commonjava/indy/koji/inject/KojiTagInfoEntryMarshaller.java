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
package org.commonjava.indy.koji.inject;

import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.commonjava.indy.koji.content.KojiTagInfoEntry;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;
import java.util.ArrayList;

public class KojiTagInfoEntryMarshaller implements MessageMarshaller<KojiTagInfoEntry>
{
    @Override
    public KojiTagInfoEntry readFrom(ProtoStreamReader reader) throws IOException
    {
        KojiTagInfoEntry entry = new KojiTagInfoEntry();
        entry.setTagInfos( reader.readCollection( "tagInfos", new ArrayList<>(), KojiTagInfo.class ) );
        return entry;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, KojiTagInfoEntry kojiTagInfoEntry) throws IOException
    {
        writer.writeCollection("tagInfos", kojiTagInfoEntry.getTagInfos(), KojiTagInfo.class);
    }

    @Override
    public Class<? extends KojiTagInfoEntry> getJavaClass()
    {
        return KojiTagInfoEntry.class;
    }

    @Override
    public String getTypeName()
    {
        return "koji.KojiTagInfoEntry";
    }
}
