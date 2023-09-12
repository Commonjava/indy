/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.kafka;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.model.core.io.IndyObjectMapper;

import javax.enterprise.inject.spi.CDI;

/**
 * A {@link Serializer} that serializes JSON using Jackson's ObjectMapper.
 */
public class KafkaObjectMapperSerializer<T>
        implements Serializer<T>
{

    private final ObjectMapper objectMapper;

    public KafkaObjectMapperSerializer()
    {
        this.objectMapper = CDI.current().select( IndyObjectMapper.class ).get();
        if ( this.objectMapper == null )
        {
            throw new IllegalArgumentException( "Error: can not get IndyObjectMapper correctly from CDI environment!" );
        }
    }

    @Override
    public void configure( Map<String, ?> configs, boolean isKey )
    {
    }

    @Override
    public byte[] serialize( String topic, T data )
    {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            objectMapper.writeValue( output, data );
            return output.toByteArray();
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void close()
    {
    }
}

