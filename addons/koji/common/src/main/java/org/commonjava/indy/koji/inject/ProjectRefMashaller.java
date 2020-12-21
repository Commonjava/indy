package org.commonjava.indy.koji.inject;

import org.commonjava.atlas.maven.ident.ref.SimpleProjectRef;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class ProjectRefMashaller implements MessageMarshaller<SimpleProjectRef>
{
    @Override
    public SimpleProjectRef readFrom( ProtoStreamReader reader ) throws IOException
    {
        String groupId = reader.readString( "groupId" );
        String artifactId = reader.readString( "artifactId" );
        return new SimpleProjectRef( groupId, artifactId );
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, SimpleProjectRef projectRef ) throws IOException
    {
        writer.writeString( "groupId", projectRef.getGroupId() );
        writer.writeString( "artifactId", projectRef.getArtifactId() );
    }

    @Override
    public Class<? extends SimpleProjectRef> getJavaClass()
    {
        return SimpleProjectRef.class;
    }

    @Override
    public String getTypeName()
    {
        return "koji.SimpleProjectRef";
    }
}
