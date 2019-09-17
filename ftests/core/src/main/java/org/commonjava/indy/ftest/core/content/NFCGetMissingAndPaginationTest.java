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
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.AbstractRepository;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.NotFoundCacheDTO;
import org.commonjava.indy.model.core.dto.NotFoundCacheInfoDTO;
import org.commonjava.indy.model.core.dto.NotFoundCacheSectionDTO;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.test.http.expect.ExpectationServer;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Hosted A, Remote B. Both has no content.</li>
 *     <li>Group G contains A and B.</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Access some missing content to generate NFC entries</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>API getAllMissing and getMissing work properly with or without pageIndex/pageSize</li>
 *     <li>API getInfo work properly for A, B and G.</li>
 * </ul>
 */
public class NFCGetMissingAndPaginationTest
                extends AbstractIndyFunctionalTest
{
    private HostedRepository hosted;

    private RemoteRepository remote;

    private Group group;

    private static final String HOSTED = "hosted_1";

    private static final String REMOTE = "remote_1";

    private static final String GROUP = "group_1";

    private static final String PATH = "org/foo/bar/{0}/bar-{0}.pom";

    private List<String> paths;

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Before
    public void setupTest() throws Exception
    {
        // Set up A, B, and G
        String changeLog = "set up";
        hosted = client.stores().create( new HostedRepository( HOSTED ), changeLog, HostedRepository.class );
        remote = client.stores().create( new RemoteRepository( REMOTE, server.formatUrl( REMOTE ) ), changeLog, RemoteRepository.class );
        group = client.stores().create( new Group( GROUP, hosted.getKey(), remote.getKey() ), changeLog, Group.class );

        paths = new ArrayList<>();
        for ( int i = 0; i < 15; i++ )
        {
            paths.add( MessageFormat.format( PATH, i ) );
        }
        Collections.sort( paths );

        // Populate NFC data
        for ( String path : paths )
        {
//            try (InputStream inputStream = client.content().get( hosted.getKey(), path ))
//            {
//            }
            try (InputStream inputStream = client.content().get( remote.getKey(), path ))
            {
            }
        }
    }

    private List<String> getPathsInPage( List<String> paths, int pageIndex, int pageSize )
    {
        List<String> pathsInPage = new ArrayList<>(  );
        for( int i = pageIndex * pageSize; i < pageSize && i < paths.size(); i++ )
        {
            pathsInPage.add( paths.get( i ) );
        }
        return pathsInPage;
    }

    private void assertThat_DtoContainsPathsForRepository( NotFoundCacheDTO dto, List<String> expected, AbstractRepository repository )
    {
        NotFoundCacheSectionDTO nfcSectionDto = dto.getSections()
                                                   .stream()
                                                   .filter( d -> d.getKey().equals( repository.getKey() ) )
                                                   .findFirst()
                                                   .orElse( null );
        assertThat( nfcSectionDto, notNullValue() );
        assertThat( nfcSectionDto.getPaths(), notNullValue() );

        Set<String> actual = nfcSectionDto.getPaths();
        logger.debug( "Actual [{}]: {}", repository.getName(), actual );

        assertThat( actual.containsAll( expected ), equalTo( true ) );
    }

    private void assertThat_DtoContainsNoneForRepository( NotFoundCacheDTO dto, AbstractRepository repository )
    {
        NotFoundCacheSectionDTO nfcSectionDto = dto.getSections()
                                                   .stream()
                                                   .filter( d -> d.getKey().equals( repository.getKey() ) )
                                                   .findFirst()
                                                   .orElse( null );
        assertThat( nfcSectionDto, nullValue() );
    }

    @Test
    public void run() throws Exception
    {
        // Get NFC cache size
//        NotFoundCacheInfoDTO info = client.module( IndyNfcClientModule.class ).getInfo( hosted.getKey() );
//        assertEquals( info.getSize(), 15 );

        NotFoundCacheInfoDTO info = client.module( IndyNfcClientModule.class ).getInfo( remote.getKey() );
        assertEquals( info.getSize(), 15 );

        info = client.module( IndyNfcClientModule.class ).getInfo( group.getKey() );
        assertEquals( info.getSize(), 15 );

        // Get NFC for hosted
//        NotFoundCacheDTO dto = client.module( IndyNfcClientModule.class )
//                                     .getAllNfcContentInStore( StoreType.hosted, hosted.getName() );
//        assertThat( dto, notNullValue() );
//        assertThat_DtoContainsPathsForRepository( dto, paths, hosted );

        // Get NFC for remote
        NotFoundCacheDTO dto = client.module( IndyNfcClientModule.class ).getAllNfcContentInStore( StoreType.remote, remote.getName() );
        assertThat( dto, notNullValue() );
        assertThat_DtoContainsPathsForRepository( dto, paths, remote );

        // Get NFC for all
        dto = client.module( IndyNfcClientModule.class ).getAllNfcContent( );
        assertThat( dto, notNullValue() );
//        assertThat_DtoContainsPathsForRepository( dto, paths, hosted );
        assertThat_DtoContainsPathsForRepository( dto, paths, remote );

        // Pagination - pageIndex starts from 0!
        int pageSize = 10;

        List<String> pageOne = getPathsInPage( paths, 0, pageSize );
        List<String> pageTwo = getPathsInPage( paths, 1, pageSize );

        // Get NFC for page one
        dto = client.module( IndyNfcClientModule.class ).getAllNfcContentInStore( StoreType.remote, remote.getName(), 0, pageSize );
        assertThat_DtoContainsPathsForRepository( dto, pageOne, remote );

        // Get NFC for page two
        dto = client.module( IndyNfcClientModule.class ).getAllNfcContentInStore( StoreType.remote, remote.getName(), 1, pageSize );
        assertThat_DtoContainsPathsForRepository( dto, pageTwo, remote );

        // Get NFC for all with paging
//        pageSize = 10;

//        dto = client.module( IndyNfcClientModule.class ).getAllNfcContent( 0, pageSize );
//        assertThat( dto, notNullValue() );
//        assertThat_DtoContainsPathsForRepository( dto, paths, hosted );

        dto = client.module( IndyNfcClientModule.class ).getAllNfcContent( 1, pageSize );
        assertThat( dto, notNullValue() );
        assertThat_DtoContainsPathsForRepository( dto, pageTwo, remote );

        // Clear NFC for hosted
//        client.module( IndyNfcClientModule.class ).clearInStore( StoreType.hosted, hosted.getName(), null );
//        dto = client.module( IndyNfcClientModule.class ).getAllNfcContent( );
//        assertThat( dto, notNullValue() );
//        assertThat_DtoContainsPathsForRepository( dto, paths, remote );
//        assertThat_DtoContainsNoneForRepository( dto, hosted );

        // Clear NFC for remote
        client.module( IndyNfcClientModule.class ).clearInStore( StoreType.remote, remote.getName(), null );
        dto = client.module( IndyNfcClientModule.class ).getAllNfcContent( );
        assertThat( dto, notNullValue() );
        assertThat_DtoContainsNoneForRepository( dto, remote );
        assertThat_DtoContainsNoneForRepository( dto, hosted );

        // Get NFC cache size and should be 0
//        info = client.module( IndyNfcClientModule.class ).getInfo( hosted.getKey() );
//        assertEquals( info.getSize(), 0 );

        info = client.module( IndyNfcClientModule.class ).getInfo( remote.getKey() );
        assertEquals( info.getSize(), 0 );

        info = client.module( IndyNfcClientModule.class ).getInfo( group.getKey() );
        assertEquals( info.getSize(), 0 );
    }
}
