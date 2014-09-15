package org.commonjava.aprox.bind.vertx.inject;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.model.StoreKey;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.impl.DefaultVertx;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@ApplicationScoped
public class BootProvider
{

    private final DefaultVertx vertx = new DefaultVertx();

    private final ObjectMapper objectMapper = new ObjectMapper().enable( SerializationFeature.INDENT_OUTPUT )
                                                                .registerModule( new ApiModule() );

    @Produces
    public Vertx getVertx()
    {
        return vertx;
    }

    @Produces
    public EventBus getEventBus()
    {
        return vertx.eventBus();
    }

    @Produces
    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    private static final class ApiModule
        extends SimpleModule
    {
        private static final long serialVersionUID = 1L;

        ApiModule()
        {
            super( "Aprox API" );
            addDeserializer( StoreKey.class, new StoreKeyDeserializer() );
            addSerializer( StoreKey.class, new StoreKeySerializer() );
        }

    }

    private static final class StoreKeyDeserializer
        extends StdDeserializer<StoreKey>
    {
        private static final long serialVersionUID = 1L;

        StoreKeyDeserializer()
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

    private static final class StoreKeySerializer
        extends StdSerializer<StoreKey>
    {
        StoreKeySerializer()
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

}
