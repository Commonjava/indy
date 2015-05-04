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
package org.commonjava.aprox.depgraph.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.aprox.depgraph.dto.MetadataBatchUpdateDTO;
import org.commonjava.aprox.depgraph.dto.MetadataUpdateDTO;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@ApplicationScoped
public class MetadataBatchUpdateSerializerModule
    extends SimpleModule
{

    private static final long serialVersionUID = 1L;

    public MetadataBatchUpdateSerializerModule()
    {
        super( "Metadata Update Serializers" );
        addSerializer( MetadataBatchUpdateDTO.class, new MetadataBatchUpdateSerializer() );
        addDeserializer( MetadataBatchUpdateDTO.class, new MetadataBatchUpdateDeserializer() );

        addSerializer( MetadataUpdateDTO.class, new MetadataUpdateSerializer() );
        addDeserializer( MetadataUpdateDTO.class, new MetadataUpdateDeserializer() );
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

    private static final class MetadataBatchUpdateDeserializer
        extends StdDeserializer<MetadataBatchUpdateDTO>
    {
        private static final long serialVersionUID = 1L;

        protected MetadataBatchUpdateDeserializer()
        {
            super( MetadataBatchUpdateDTO.class );
        }

        @Override
        public MetadataBatchUpdateDTO deserialize( final JsonParser jp, final DeserializationContext ctxt )
            throws IOException, JsonProcessingException
        {
            final Map<ProjectVersionRef, MetadataUpdateDTO> updates = new HashMap<>();
            JsonToken token = null;
            while ( ( token = jp.nextToken() ) != JsonToken.END_OBJECT )
            {
                if ( token == JsonToken.FIELD_NAME )
                {
                    final ProjectVersionRef ref = ProjectVersionRef.parse( jp.getCurrentName() );
                    final Map<String, String> metadata = new HashMap<>();

                    while ( ( token = jp.nextToken() ) != JsonToken.END_OBJECT )
                    {
                        if ( token == JsonToken.FIELD_NAME )
                        {
                            final String key = jp.getCurrentName();
                            final String value = jp.nextTextValue();
                            metadata.put( key, value );
                        }
                    }

                    updates.put( ref, new MetadataUpdateDTO( metadata ) );
                }
            }

            return new MetadataBatchUpdateDTO( updates );
        }
    }

    private static final class MetadataBatchUpdateSerializer
        extends StdSerializer<MetadataBatchUpdateDTO>
    {
        protected MetadataBatchUpdateSerializer()
        {
            super( MetadataBatchUpdateDTO.class );
        }

        @Override
        public void serialize( final MetadataBatchUpdateDTO value, final JsonGenerator gen,
                               final SerializerProvider provider )
            throws IOException, JsonGenerationException
        {
            gen.writeStartObject();
            for ( final Map.Entry<ProjectVersionRef, MetadataUpdateDTO> projectEntry : value )
            {
                gen.writeObjectFieldStart( projectEntry.getKey()
                                                       .toString() );
                for ( final Map.Entry<String, String> mdEntry : projectEntry.getValue() )
                {
                    gen.writeStringField( mdEntry.getKey(), mdEntry.getValue() );
                }
                gen.writeEndObject();
            }
            gen.writeEndObject();
        }
    }

    private static final class MetadataUpdateDeserializer
        extends StdDeserializer<MetadataUpdateDTO>
    {
        private static final long serialVersionUID = 1L;

        protected MetadataUpdateDeserializer()
        {
            super( MetadataUpdateDTO.class );
        }

        @Override
        public MetadataUpdateDTO deserialize( final JsonParser jp, final DeserializationContext ctxt )
            throws IOException, JsonProcessingException
        {
            final Map<String, String> metadata = new HashMap<>();
            JsonToken token = null;
            while ( ( token = jp.nextToken() ) != JsonToken.END_OBJECT )
            {
                if ( token == JsonToken.FIELD_NAME )
                {
                    final String key = jp.getCurrentName();
                    final String value = jp.nextTextValue();
                    metadata.put( key, value );
                }
            }

            return new MetadataUpdateDTO( metadata );
        }
    }

    private static final class MetadataUpdateSerializer
        extends StdSerializer<MetadataUpdateDTO>
    {
        protected MetadataUpdateSerializer()
        {
            super( MetadataUpdateDTO.class );
        }

        @Override
        public void serialize( final MetadataUpdateDTO value, final JsonGenerator gen, final SerializerProvider provider )
            throws IOException, JsonGenerationException
        {
            gen.writeStartObject();
            for ( final Map.Entry<String, String> mdEntry : value )
            {
                gen.writeStringField( mdEntry.getKey(), mdEntry.getValue() );
            }
            gen.writeEndObject();
        }
    }

}
