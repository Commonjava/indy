package org.commonjava.aprox.model.core.io;

import java.io.IOException;

import org.commonjava.aprox.model.core.StoreKey;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public final class StoreKeySerializer
    extends StdSerializer<StoreKey>
{
    public StoreKeySerializer()
    {
        super( StoreKey.class );
    }

    @Override
    public void serialize( final StoreKey key, final JsonGenerator generator, final SerializerProvider provider )
        throws IOException, JsonProcessingException
    {
        generator.writeString( key.toString() );
    }
}