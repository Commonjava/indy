/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.model.core.io;

import java.io.IOException;

import org.commonjava.indy.model.core.StoreKey;

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