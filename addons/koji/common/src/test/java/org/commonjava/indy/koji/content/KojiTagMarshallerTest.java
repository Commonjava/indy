package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import org.commonjava.indy.koji.inject.KojiTagInfoEntryMarshaller;
import org.commonjava.indy.koji.inject.KojiTagInfoMarshaller;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
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
        assertTrue( ((KojiTagInfoEntry) out).getTagInfos().contains("x86") );

    }

}
