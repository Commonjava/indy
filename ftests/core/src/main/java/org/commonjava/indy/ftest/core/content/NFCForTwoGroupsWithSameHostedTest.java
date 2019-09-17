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
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.NotFoundCacheDTO;
import org.commonjava.indy.model.core.dto.NotFoundCacheSectionDTO;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
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
 *     <li>Groups A and B</li>
 *     <li>Hosted repository X</li>
 *     <li>Both A and B contain X as a member</li>
 *     <li>Path P doesn't exist in X</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Step-1: Resolve P from A</li>
 *     <li>Step-2: Upload P to X via B</li>
 *     <li>Step-3: Resolve P from A again</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>After step-1: A should get an NFC entry for P</li>
 *     <li>After step-3: NFC entry for P in A should have been cleared</li>
 * </ul>
 */
public class NFCForTwoGroupsWithSameHostedTest
        extends AbstractContentManagementTest
{
    private HostedRepository x;

    private static final String NAME_X = "hosted_x";

    private Group a;

    private static final String NAME_A = "group_a";

    private Group b;

    private static final String NAME_B = "group_b";

    private static final String PATH = "org/foo/bar/1/bar-1.jar";

    @Before
    public void setupTest()
            throws Exception
    {
        String change = "test setup";
        x = client.stores()
                  .create( new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, NAME_X ), change,
                           HostedRepository.class );

        a = client.stores()
                  .create( new Group( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, NAME_A, x.getKey() ), change,
                           Group.class );
        b = client.stores()
                  .create( new Group( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, NAME_B, x.getKey() ), change,
                           Group.class );
    }

    @Test
    public void run()
            throws Exception
    {
        try (InputStream inputStream = client.content().get( a.getKey(), PATH ))
        {
            assertThat( inputStream, nullValue() );
        }

//        NotFoundCacheDTO dto = client.module( IndyNfcClientModule.class ).getAllNfcContentInStore( StoreType.group, a.getName() );
//
//        assertThat( dto, notNullValue() );
//        assertThat( dto.getSections(), notNullValue() );
//        NotFoundCacheSectionDTO nfcSectionDto = dto.getSections().stream().findFirst().orElse( null );
//        assertThat( nfcSectionDto, notNullValue() );
//        assertThat( nfcSectionDto.getPaths(), notNullValue() );
//        assertThat( nfcSectionDto.getPaths().contains( PATH ), equalTo( true ) );

//        dto = client.module( IndyNfcClientModule.class ).getAllNfcContentInStore( StoreType.hosted, x.getName() );
//
//        assertThat( dto, notNullValue() );
//        assertThat( dto.getSections(), notNullValue() );
//        nfcSectionDto = dto.getSections().stream().findFirst().orElse( null );
//        assertThat( nfcSectionDto, notNullValue() );
//        assertThat( nfcSectionDto.getPaths(), notNullValue() );
//        assertThat( nfcSectionDto.getPaths().contains( PATH ), equalTo( true ) );

        client.content().store( b.getKey(), PATH, new ByteArrayInputStream( "This is the pom".getBytes() ) );

        try (InputStream inputStream = client.content().get( b.getKey(), PATH ))
        {
            assertThat( inputStream, notNullValue() );
        }

//        dto = client.module( IndyNfcClientModule.class ).getAllNfcContentInStore( StoreType.hosted, x.getName() );
//
//        assertThat( dto, notNullValue() );
//        assertThat( dto.getSections(), notNullValue() );
//        nfcSectionDto =
//                dto.getSections().stream().findFirst().orElse( null );
//        assertThat( nfcSectionDto, notNullValue() );
//        assertThat( nfcSectionDto.getPaths(), nullValue() );

        try (InputStream inputStream = client.content().get( a.getKey(), PATH ))
        {
            assertThat( inputStream, notNullValue() );
        }

        NotFoundCacheDTO dto = client.module( IndyNfcClientModule.class ).getAllNfcContentInStore( StoreType.group, a.getName() );

        NotFoundCacheSectionDTO nfcSectionDto = dto.getSections().stream().findFirst().orElse( null );
        assertThat( nfcSectionDto, nullValue() );
    }
}
