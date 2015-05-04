/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.model.core.io;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.aprox.model.core.StoreKey;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@ApplicationScoped
public class ApiSerializerModule
    extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    public ApiSerializerModule()
    {
        super( "AProx Core API" );
        addDeserializer( StoreKey.class, new StoreKeyDeserializer() );
        addSerializer( StoreKey.class, new StoreKeySerializer() );
    }

    @Override
    public int hashCode()
    {
        return getClass().getSimpleName()
                         .hashCode() + 17;
    }

    @Override
    public boolean equals( final Object other )
    {
        return getClass().equals( other.getClass() );
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
