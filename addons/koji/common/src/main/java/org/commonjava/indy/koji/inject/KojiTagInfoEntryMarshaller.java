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
