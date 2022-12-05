/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
