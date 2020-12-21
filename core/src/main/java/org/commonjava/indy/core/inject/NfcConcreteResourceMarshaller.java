package org.commonjava.indy.core.inject;

import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class NfcConcreteResourceMarshaller implements MessageMarshaller<NfcConcreteResourceWrapper>
{
    @Override
    public NfcConcreteResourceWrapper readFrom( ProtoStreamReader reader ) throws IOException
    {
        NfcConcreteResourceWrapper wrapper = new NfcConcreteResourceWrapper();
        wrapper.setLocation( reader.readString( "location" ) );
        wrapper.setPath( reader.readString( "path" ) );
        wrapper.setTimeout( reader.readLong( "timeout" ) );
        return wrapper;
    }

    @Override
    public void writeTo( ProtoStreamWriter writer, NfcConcreteResourceWrapper nfcConcreteResourceWrapper )
                    throws IOException
    {
        writer.writeString( "location", nfcConcreteResourceWrapper.getLocation() );
        writer.writeString( "path", nfcConcreteResourceWrapper.getPath() );
        writer.writeLong( "timeout", nfcConcreteResourceWrapper.getTimeout() );
    }

    @Override
    public Class<? extends NfcConcreteResourceWrapper> getJavaClass()
    {
        return NfcConcreteResourceWrapper.class;
    }

    @Override
    public String getTypeName()
    {
        return "nfc.ConcreteResourceWrapper";
    }
}
