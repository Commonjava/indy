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
package org.commonjava.indy.pkg.npm.content;

import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.model.VersionMetadata;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class VersionMetaReadTest
{
    final IndyObjectMapper mapper = new IndyObjectMapper( true );

    @Test
    public void testNormalLicense() throws IOException {
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("metadata/package-2.json")) {
            VersionMetadata versionMetadata = mapper.readValue(input, VersionMetadata.class);
            assertTrue(versionMetadata.getLicense().getType().equals("MIT"));
        }
    }

    @Test
    public void testMultipleLicense() throws IOException {
        try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("metadata/pause-stream.json")) {
            VersionMetadata versionMetadata = mapper.readValue(input, VersionMetadata.class);
            assertTrue(versionMetadata.getLicense().getType().equals("(MIT OR Apache2)"));
        }
    }
}
