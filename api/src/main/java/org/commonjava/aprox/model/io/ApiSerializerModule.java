/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.model.io;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.aprox.model.StoreKey;

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
