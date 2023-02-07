/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.promote.ftest;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Given:
 * b1 is hosted and contains pom 1.0
 * g1 contains hosted:b1
 * g1 contains hosted:target
 * g1 name is 'build-1'
 * g_builds contains hosted:target
 *
 * When:
 * promote b1 to target
 *
 * Then:
 * GET metadata from g1, contains version 1.0
 * GET metadata from g_builds, contains version 1.0
 *
 * When:
 * b2 is hosted and contains pom 2.0
 * promote b2 to target
 *
 * Then:
 * GET metadata from g1, contains version 1.0 (g1 metadata is not updated)
 * GET metadata from g_builds, contains version 1.0 and 2.0
 */
public class PromoteWithExcludeFilterTest
                extends AbstractPromotionManagerTest
{
    private static final String METADATA_PATH = "/org/foo/bar/maven-metadata.xml";

    private static final String POM_PATH_TEMPLATE = "/org/foo/bar/%version%/bar-%version%.pom";

    /* @formatter:off */
    private static final String POM_CONTENT_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<project>\n" +
                    "  <modelVersion>4.0.0</modelVersion>\n" +
                    "  <groupId>org.foo</groupId>\n" +
                    "  <artifactId>bar</artifactId>\n" +
                    "  <version>%version%</version>\n" +
                    "  <name>Bar</name>\n" +
                    "</project>\n";
    /* @formatter:on */

    Group gBuilds;

    Group g1;

    HostedRepository b1, b2;

    HostedRepository target;

    @Before
    @Override
    public void setupRepos() throws Exception
    {
        String changelog = "Setup " + name.getMethodName();

        // create b1
        b1 = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "b1" );
        client.stores().create( b1, changelog, HostedRepository.class );

        // store pom 1.0 to b1
        client.content()
              .store( b1.getKey(), POM_PATH_TEMPLATE.replace( "%version%", "1.0" ),
                      new ByteArrayInputStream( POM_CONTENT_TEMPLATE.replace( "%version%", "1.0" ).getBytes() ) );

        // create target
        target = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "target" );
        client.stores().create( target, changelog, HostedRepository.class );

        // create g1
        g1 = new Group( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "build-1", b1.getKey(), target.getKey() );
        client.stores().create( g1, changelog, Group.class );

        // create b2
        b2 = new HostedRepository( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "b2" );
        client.stores().create( b2, changelog, HostedRepository.class );

        // store pom 2.0 to b2
        client.content()
              .store( b2.getKey(), POM_PATH_TEMPLATE.replace( "%version%", "2.0" ),
                      new ByteArrayInputStream( POM_CONTENT_TEMPLATE.replace( "%version%", "2.0" ).getBytes() ) );

        gBuilds = new Group( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, "g-builds", target.getKey() );
        client.stores().create( gBuilds, changelog, Group.class );
    }

    @Test
    public void run() throws Exception
    {
        // Promote b1 to target
        PathsPromoteResult result = client.module( IndyPromoteClientModule.class )
                                          .promoteByPath( new PathsPromoteRequest( b1.getKey(), target.getKey() ) );
        assertThat( result.getError(), nullValue() );

        String g1Metadata = getMetadataString( g1.getKey() );
        String gBuildsMetadata = getMetadataString( gBuilds.getKey() );
/*
        System.out.println( "g1 >>>\n" + g1Metadata );
        System.out.println( "g-builds >>>\n" + gBuildsMetadata );
*/
        assertThat( client.content().exists( g1.getKey(), METADATA_PATH ), equalTo( true ) );
        assertThat( client.content().exists( gBuilds.getKey(), METADATA_PATH ), equalTo( true ) );

        // Promote b2 to target
        result = client.module( IndyPromoteClientModule.class )
                       .promoteByPath( new PathsPromoteRequest( b2.getKey(), target.getKey() ) );
        assertThat( result.getError(), nullValue() );

        g1Metadata = getMetadataString( g1.getKey() );
        gBuildsMetadata = getMetadataString( gBuilds.getKey() );
/*
        System.out.println( "g1 >>>\n" + g1Metadata );
        System.out.println( "g-builds >>>\n" + gBuildsMetadata );
*/
        assertThat( client.content().exists( g1.getKey(), METADATA_PATH ), equalTo( true ) );
        assertThat( client.content().exists( gBuilds.getKey(), METADATA_PATH ), equalTo( true ) );

        // g_builds metadata is updated but g1 is excluded during metadata clean-up
        assertThat( g1Metadata.contains( "<latest>2.0</latest>" ), equalTo( false ) );
        assertThat( gBuildsMetadata.contains( "<latest>2.0</latest>" ), equalTo( true ) );
    }

    private String getMetadataString( StoreKey key ) throws Exception
    {
        try ( InputStream inputStream = client.content().get( key, METADATA_PATH ) )
        {
            return IOUtils.toString( inputStream );
        }
    }

}
