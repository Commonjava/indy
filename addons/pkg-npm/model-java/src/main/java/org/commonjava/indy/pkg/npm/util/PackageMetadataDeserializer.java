/**
 * Copyright (C) 2017 Red Hat, Inc. (jdcasey@commonjava.org)
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

package org.commonjava.indy.pkg.npm.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;

import java.io.IOException;

public class PackageMetadataDeserializer
                extends StdDeserializer<PackageMetadata>
{

    private static final long serialVersionUID = 1L;

    public PackageMetadataDeserializer()
    {
        super( PackageMetadata.class );
    }

    @Override
    public PackageMetadata deserialize( JsonParser parser, DeserializationContext context )
                    throws IOException, JsonProcessingException
    {
        //JsonNode node = jp.getCodec().readTree( jp );
        final String keyStr = parser.getText();
        return PackageMetadata.fromString( keyStr );
    }
}

