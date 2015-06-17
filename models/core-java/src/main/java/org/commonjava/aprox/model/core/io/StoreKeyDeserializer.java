package org.commonjava.aprox.model.core.io;

import java.io.IOException;

import org.commonjava.aprox.model.core.StoreKey;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public final class StoreKeyDeserializer
    extends StdDeserializer<StoreKey>
{
    private static final long serialVersionUID = 1L;

    public StoreKeyDeserializer()
    {
        super( StoreKey.class );
    }

    @Override
    public StoreKey deserialize( final JsonParser parser, final DeserializationContext context )
        throws IOException, JsonProcessingException
    {
        final String keyStr = parser.getText();
        return StoreKey.fromString( keyStr );
    }

}