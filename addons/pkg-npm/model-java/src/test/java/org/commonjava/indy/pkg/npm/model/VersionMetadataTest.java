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
package org.commonjava.indy.pkg.npm.model;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class VersionMetadataTest
{
    @Test
    public void roundTripJson() throws Exception
    {
        final IndyObjectMapper mapper = new IndyObjectMapper( true );
        final VersionMetadata metadata = new VersionMetadata( "test", "1.1.0" );
        final String json = mapper.writeValueAsString( metadata );

        System.out.println( json );

        final VersionMetadata result = mapper.readValue( json, VersionMetadata.class );
        assertThat( result.getName(), equalTo( metadata.getName() ) );
        assertThat( result.getVersion(), equalTo( metadata.getVersion() ) );
    }

    @Test
    public void ignoreCouchDBJsonDataTest() throws Exception
    {

        final IndyObjectMapper mapper = new IndyObjectMapper( true );
        String json = IOUtils.toString(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream( "test-package.json" ) );

        final PackageMetadata result = mapper.readValue( json, PackageMetadata.class );
        final VersionMetadata version = result.getVersions().get( "1.5.1" );
        final String jsonResult = mapper.writeValueAsString( version );

        assertThat( jsonResult.contains( "_id" ), equalTo( false ) );

    }
}
