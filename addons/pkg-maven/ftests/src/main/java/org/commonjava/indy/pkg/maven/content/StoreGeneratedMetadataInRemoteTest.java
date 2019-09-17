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
package org.commonjava.indy.pkg.maven.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 *
 * Store generated metadata in remote repos storage when requested as part of a group metadata generation.
 *
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Remote repo A which contains path_1 foo/bar/1/bar-1.pom</li>
 *     <li>Hosted repo B which contains path_2 foo/bar/2/bar-2.pom and foo/bar/maven-metadata.xml</li>
 *     <li>Group G contains A and B.</li>
 </li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Path foo/bar/maven-metadata.xml is requested from G</li>
 *     <li>Path foo/bar/maven-metadata.xml is requested from A</li>
 *     <li>Repo A is deleted</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>Indy generates the meta files and stores it for both A and G.</li>
 *     <li>Indy can get maven-metadata.xml from A</li>
 *     <li>Group G metadata is updated after deleting A</li>
 * </ul>
 */
public class StoreGeneratedMetadataInRemoteTest
                extends AbstractIndyFunctionalTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    private final String REMOTE = "remote-A";

    private final String HOSTED = "hosted-B";

    private final String GROUP = "group-G";

    private final String path_1 = "org/foo/bar/1.0/bar-1.0.pom";

    private final String path_2 = "org/foo/bar/2.0/bar-2.0.pom";

    private final String metaPath = "org/foo/bar/maven-metadata.xml";

    /* @formatter:off */
    final String metaContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>2.0</latest>\n" +
        "    <release>2.0</release>\n" +
        "    <versions>\n" +
        "      <version>2.0</version>\n" +
        "    </versions>\n" +
        "  </versioning>\n" +
        "</metadata>\n";
    /* @formatter:on */

    private final String pomContent_1 = "<project><modelVersion>4.0.0</modelVersion>"
                    + "<groupId>org.foo</groupId><artifactId>bar</artifactId><version>1.0</version></project>";

    private final String pomContent_2 = "<project><modelVersion>4.0.0</modelVersion>"
                    + "<groupId>org.foo</groupId><artifactId>bar</artifactId><version>2.0</version></project>";

    @Test
    public void run() throws Exception
    {
        server.expect( server.formatUrl( REMOTE, path_1 ), 200, pomContent_1 );

        RemoteRepository remote1 = new RemoteRepository( MAVEN_PKG_KEY, REMOTE, server.formatUrl( REMOTE ) );
        remote1 = client.stores().create( remote1, "remote A", RemoteRepository.class );

        HostedRepository hosted1 = client.stores().create( new HostedRepository( MAVEN_PKG_KEY, HOSTED ), "hosted B", HostedRepository.class );
        client.content().store( hosted1.getKey(), path_2, new ByteArrayInputStream( pomContent_2.getBytes() ) );
        client.content().store( hosted1.getKey(), metaPath, new ByteArrayInputStream( metaContent.getBytes() ) );

        Group g = new Group( MAVEN_PKG_KEY, GROUP, remote1.getKey(), hosted1.getKey() );
        g = client.stores().create( g, "group G", Group.class );

        // MUST hit the .pom first. This is needed to populate org/foo/bar/1.0 folder in order to generate metadata.xml
        try (final InputStream stream = client.content().get( remote1.getKey(), path_1 ))
        {
        }

        // Get meta from group. Contains both version 1.0 and 2.0
        try (final InputStream stream = client.content().get( g.getKey(), metaPath ))
        {
            String meta = IOUtils.toString( stream );
            logger.debug( "Group meta >>>>\n" + meta );
            assertTrue( meta.contains( "<version>1.0</version>" ) );
            assertTrue( meta.contains( "<version>2.0</version>" ) );
        }

        // Get meta from remote. Contains version 1.0
        try (final InputStream stream = client.content().get( remote1.getKey(), metaPath ))
        {
            assertThat( stream, notNullValue() );
            String meta = IOUtils.toString( stream );
            logger.debug( "Remote A meta >>>>\n" + meta );
            assertTrue( meta.contains( "<version>1.0</version>" ) );
        }

        // Delete remote A
        client.stores().delete( remote1.getKey(), "delete A" );

        /*
         * Disable it has same effect

        remote1.setDisabled( true );
        client.stores().update( remote1, "disable A" );
        */

        waitForEventPropagation();

        // Get meta from group again. Contains only version 2.0, no 1.0
        try (final InputStream stream = client.content().get( g.getKey(), metaPath ))
        {
            assertThat( stream, notNullValue() );
            String meta = IOUtils.toString( stream );
            logger.debug( "Group meta after deleting A >>>>\n" + meta );
            assertFalse( meta.contains( "<version>1.0</version>" ) );
        }
    }
}
