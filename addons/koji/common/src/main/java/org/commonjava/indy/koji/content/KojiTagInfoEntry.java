package org.commonjava.indy.koji.content;

import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;

import java.util.List;

public class KojiTagInfoEntry
{

    List<KojiTagInfo> tagInfos;

    public KojiTagInfoEntry(){}

    public KojiTagInfoEntry(List<KojiTagInfo> tagInfos)
    {
        this.tagInfos = tagInfos;
    }

    public List<KojiTagInfo> getTagInfos()
    {
        return tagInfos;
    }

    public void setTagInfos(List<KojiTagInfo> tagInfos)
    {
        this.tagInfos = tagInfos;
    }
}
