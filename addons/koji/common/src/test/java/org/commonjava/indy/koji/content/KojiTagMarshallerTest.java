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
package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.commonjava.indy.koji.inject.KojiTagInfoEntryMarshaller;
import org.commonjava.indy.koji.inject.KojiTagInfoMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class KojiTagMarshallerTest

{
    @Test
    public void tagInfoMarshaller() throws Exception
    {
        SerializationContext ctx = ProtobufUtil.newSerializationContext();

        ctx.registerProtoFiles( FileDescriptorSource.fromResources( "koji_taginfo.proto" ) );
        ctx.registerMarshaller( new KojiTagInfoMarshaller() );
        ctx.registerMarshaller( new KojiTagInfoEntryMarshaller() );

        List<KojiTagInfo> tagInfos = new ArrayList<>();
        KojiTagInfo tagInfo = new KojiTagInfo();
        tagInfo.setArches(Arrays.asList("x86_64", "ppc64le"));
        tagInfos.add(tagInfo);

        KojiTagInfoEntry entry = new KojiTagInfoEntry(tagInfos);

        byte[] bytes = ProtobufUtil.toWrappedByteArray(ctx, entry);
        Object out = ProtobufUtil.fromWrappedByteArray(ctx, bytes);

        assertTrue( out instanceof KojiTagInfoEntry );
        assertTrue( !((KojiTagInfoEntry) out).getTagInfos().isEmpty() );
        assertTrue( ((KojiTagInfoEntry) out).getTagInfos().get(0).getArches().contains("x86_64") );

    }

}
