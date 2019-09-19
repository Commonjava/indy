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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.client.core.module.IndyNfcClientModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.NotFoundCacheDTO;
import org.commonjava.indy.model.core.dto.NotFoundCacheSectionDTO;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A hosted with some content</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Access a missing content in the hosted</li>
 *     <li>After access store the missing content in hosted</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The content is missing</li>
 *     <li>The missing content is added to the NFC</li>
 *     <li>After store the missing content, entry in NFC for this content removed</li>
 * </ul>
 */
@Ignore( "Not adding hosted repo paths to NFC any more since performance is not much better than hitting the filesystem, and it consumes memory" )
public class HostedMissingAddToNFCTest
        extends AbstractContentManagementTest
{
    private HostedRepository hosted;

    private static final String HOSTED = "hosted";

    private static final String JAR_PATH = "org/foo/bar/1/bar-1.jar";

    private static final String POM_PATH = "org/foo/bar/1/bar-1.pom";

    private static final String META_PATH = "org/foo/bar/maven-metadata.xml";

    @Before
    public void setupTest()
            throws Exception
    {
        String change = "test setup";
        hosted = client.stores().create( new HostedRepository( HOSTED ), change, HostedRepository.class );
        client.content().store( hosted.getKey(), JAR_PATH, new ByteArrayInputStream( "This is the jar".getBytes() ) );
    }

    @Test
    public void run()
            throws Exception
    {
        try (InputStream inputStream = client.content().get( hosted.getKey(), JAR_PATH ))
        {
            assertThat( inputStream, notNullValue() );
        }

        try (InputStream inputStream = client.content().get( hosted.getKey(), POM_PATH ))
        {
            assertThat( inputStream, IsNull.nullValue() );
        }

        NotFoundCacheDTO dto = client.module( IndyNfcClientModule.class )
                                     .getAllNfcContentInStore( StoreType.hosted, hosted.getName() );

        assertThat( dto, notNullValue() );
        assertThat( dto.getSections(), notNullValue() );

        NotFoundCacheSectionDTO nfcSectionDto = dto.getSections()
                                                   .stream()
                                                   .filter( d -> d.getKey().equals( hosted.getKey() ) )
                                                   .findFirst()
                                                   .orElse( null );
        assertThat( nfcSectionDto, notNullValue() );
        assertThat( nfcSectionDto.getPaths(), notNullValue() );
        assertThat( nfcSectionDto.getPaths().contains( POM_PATH ), equalTo( true ) );

        client.content().store( hosted.getKey(), POM_PATH, new ByteArrayInputStream( "This is the pom".getBytes() ) );

        try (InputStream inputStream = client.content().get( hosted.getKey(), POM_PATH ))
        {
            assertThat( inputStream, notNullValue() );
        }

        dto = client.module( IndyNfcClientModule.class )
                    .getAllNfcContentInStore( StoreType.hosted, hosted.getName() );

        assertThat( dto, notNullValue() );
        assertThat( dto.getSections(), notNullValue() );

        nfcSectionDto = dto.getSections()
                           .stream()
                           .filter( d -> d.getKey().equals( hosted.getKey() ) )
                           .findFirst()
                           .orElse( null );
        assertThat( nfcSectionDto, notNullValue() );
        assertThat( nfcSectionDto.getPaths(), nullValue() );

        try (InputStream inputStream = client.content().get( hosted.getKey(), META_PATH ))
        {
            assertThat( inputStream, notNullValue() );
        }
    }
}
