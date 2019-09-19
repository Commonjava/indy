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
import static org.junit.Assert.assertTrue;

public class PackageMetadataTest
{
    @Test
    public void roundTripJson() throws Exception
    {
        final IndyObjectMapper mapper = new IndyObjectMapper( true );
        final PackageMetadata metadata = new PackageMetadata( "test" );
        final String json = mapper.writeValueAsString( metadata );

        System.out.println( json );

        final PackageMetadata result = mapper.readValue( json, PackageMetadata.class );
        assertThat( result.getName(), equalTo( metadata.getName() ) );
    }

    @Test
    public void realPackageJsonTest() throws Exception
    {
        final IndyObjectMapper mapper = new IndyObjectMapper( true );
        String json = IOUtils.toString(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream( "test-package.json" ) );

        final PackageMetadata result = mapper.readValue( json, PackageMetadata.class );
        assertThat( result.getDistTags().getBeta(), equalTo( "3.2.1" ) );

        assertTrue( result.getVersions().containsKey( "1.5.1" ) );
        assertThat( result.getVersions().get( "1.5.1" ).getNpmVersion(), equalTo( "0.3.15" ) );

        assertThat( result.getRepository().getType(), equalTo( "git" ) );

        final String jsonResult = mapper.writeValueAsString( result );
        System.out.println( jsonResult );
    }

    @Test
    public void ignoreCouchDBJsonDataTest() throws Exception
    {

        final IndyObjectMapper mapper = new IndyObjectMapper( true );
        String json = IOUtils.toString(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream( "test-package.json" ) );

        final PackageMetadata result = mapper.readValue( json, PackageMetadata.class );
        final String jsonResult = mapper.writeValueAsString( result );

        assertThat( jsonResult.contains( "_rev" ), equalTo( false ) );
        assertThat( jsonResult.contains( "_id" ), equalTo( false ) );
        assertThat( jsonResult.contains( "_attachments" ), equalTo( false ) );
    }
}
