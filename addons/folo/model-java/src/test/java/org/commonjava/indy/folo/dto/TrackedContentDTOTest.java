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

import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class TrackedContentDTOTest
{
    @Test
    public void jsonRoundTrip_Empty()
            throws IOException
    {
        TrackedContentDTO in =
                new TrackedContentDTO( new TrackingKey( "foo" ), Collections.emptySet(), Collections.emptySet() );

        assertRoundTrip( in, ( out ) -> {
        } );
    }

    @Test
    public void jsonRoundTrip_DownloadsAndUploads()
            throws IOException
    {
        Set<TrackedContentEntryDTO> downloads =
                setOf( new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), AccessChannel.MAVEN_REPO,
                                                   "/path/to/my.pom" ),
                       new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo2" ),
                                                   AccessChannel.MAVEN_REPO, "/path/to/another/file.pom" ) );

        Set<TrackedContentEntryDTO> uploads =
                setOf( new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo3" ),
                                                   AccessChannel.MAVEN_REPO, "/path/to/third/artifact.pom" ),
                       new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo3" ),
                                                   AccessChannel.MAVEN_REPO, "/path/to/fourth/project.pom" ) );

        TrackedContentDTO in = new TrackedContentDTO( new TrackingKey( "key" ), uploads, downloads );

        assertRoundTrip( in, ( out ) -> {
            assertContents( out.getUploads(), in.getUploads() );
            assertContents( out.getDownloads(), in.getDownloads() );
        } );
    }

    @Test
    public void jsonRoundTrip_DownloadsNoUploads()
            throws IOException
    {
        Set<TrackedContentEntryDTO> downloads =
                setOf( new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), AccessChannel.MAVEN_REPO,
                                                   "/path/to/my.pom" ),
                       new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo2" ),
                                                   AccessChannel.MAVEN_REPO, "/path/to/another/file.pom" ) );

        TrackedContentDTO in = new TrackedContentDTO( new TrackingKey( "key" ), Collections.emptySet(), downloads );

        assertRoundTrip( in, ( out ) -> {
            assertNullOrEmpty( out.getUploads() );
            assertContents( out.getDownloads(), in.getDownloads() );
        } );
    }

    @Test
    public void jsonRoundTrip_UploadsNoDownloads()
            throws IOException
    {
        Set<TrackedContentEntryDTO> uploads =
                setOf( new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo" ), AccessChannel.MAVEN_REPO,
                                                   "/path/to/my.pom" ),
                       new TrackedContentEntryDTO( new StoreKey( StoreType.remote, "foo2" ),
                                                   AccessChannel.MAVEN_REPO, "/path/to/another/file.pom" ) );

        TrackedContentDTO in = new TrackedContentDTO( new TrackingKey( "key" ), uploads, Collections.emptySet() );

        assertRoundTrip( in, ( out ) -> {
            assertContents( out.getUploads(), in.getUploads() );
            assertNullOrEmpty( out.getDownloads() );
        } );
    }

    private void assertContents( final Set<TrackedContentEntryDTO> result, final Set<TrackedContentEntryDTO> test )
    {
        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( test.size() ) );

        test.forEach(
                ( entry ) -> assertThat( "Deserialized result doesn't contain: " + entry, result.contains( entry ),
                                         equalTo( true ) ) );
    }

    private Set<TrackedContentEntryDTO> setOf( final TrackedContentEntryDTO... entries )
    {
        return Stream.of( entries ).collect( Collectors.toSet() );
    }

    private void assertNullOrEmpty( final Set<TrackedContentEntryDTO> values )
    {
        assertThat( values == null || values.isEmpty(), equalTo( true ) );
    }

    private void assertRoundTrip( final TrackedContentDTO in, final Consumer<TrackedContentDTO> extraAssertions )
            throws IOException
    {
        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        TrackedContentDTO out = mapper.readValue( json, TrackedContentDTO.class );

        assertThat( out, notNullValue() );
        assertThat( out.getKey(), equalTo( in.getKey() ) );

        if ( extraAssertions != null )
        {
            extraAssertions.accept( out );
        }

        assertThat( out, equalTo( in ) );
    }
}
