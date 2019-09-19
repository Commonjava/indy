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
package org.commonjava.indy.folo.dto;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class TrackedContentEntryDTOTest
{
    @Test
    public void jsonRoundTrip_Basic()
            throws IOException
    {
        TrackedContentEntryDTO in =
                new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), AccessChannel.MAVEN_REPO,
                                            "/path/to/my.pom" );

        assertRoundTrip( in, (out)->{} );
    }

    @Test
    public void jsonRoundTrip_Checksums()
            throws IOException
    {
        TrackedContentEntryDTO in =
                new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), AccessChannel.MAVEN_REPO,
                                            "/path/to/my.pom" );

        String content = "This is a test string";
        in.setMd5( DigestUtils.md5Hex( content ) );
        in.setSha256( DigestUtils.sha256Hex( content ) );
        in.setSha1( DigestUtils.shaHex( content ) );

        assertRoundTrip( in, (out)->{
            assertThat( out.getMd5(), equalTo( in.getMd5() ) );
            assertThat( out.getSha256(), equalTo( in.getSha256() ) );
            assertThat( out.getSha1(), equalTo( in.getSha1() ) );
        } );
    }
    @Test
    public void jsonRoundTrip_size()
            throws IOException
    {
        TrackedContentEntryDTO in =
                new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), AccessChannel.MAVEN_REPO, "/path/to/my.pom" );

        in.setSize(1234123L);

        assertRoundTrip( in, (out)-> assertThat( out.getSize(), equalTo( in.getSize() ) ) );
    }

    @Test
    public void jsonRoundTrip_Urls()
            throws IOException
    {
        TrackedContentEntryDTO in =
                new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), AccessChannel.MAVEN_REPO,
                                            "/path/to/my.pom" );

        in.setLocalUrl( "http://localhost:8080/api/remote/foo/path/to/my.pom" );
        in.setOriginUrl( "http://foo.com/repo/path/to/my.pom" );

        assertRoundTrip( in, (out)->{
            assertThat( out.getLocalUrl(), equalTo( out.getLocalUrl() ) );
            assertThat( out.getOriginUrl(), equalTo( out.getOriginUrl() ) );
        } );
    }

    private void assertRoundTrip( final TrackedContentEntryDTO in, final Consumer<TrackedContentEntryDTO> extraAssertions )
            throws IOException
    {
        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        TrackedContentEntryDTO out = mapper.readValue( json, TrackedContentEntryDTO.class );

        assertThat( out, notNullValue() );
        assertThat( out.getStoreKey(), equalTo( in.getStoreKey() ) );
        assertThat( out.getPath(), equalTo( in.getPath() ) );

        if ( extraAssertions != null )
        {
            extraAssertions.accept( out );
        }

        assertThat( out, equalTo( in ) );
    }
}
