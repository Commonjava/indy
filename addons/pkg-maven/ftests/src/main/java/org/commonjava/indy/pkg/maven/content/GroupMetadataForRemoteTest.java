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
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * <b>GIVEN:</b>
 * GroupId: o.a.maven.plugins
 * ArtifactId: maven-antrun-plugin
 *
 * <li>
 *     Remote repo A contains paths and content for:
 *
 *       org/foo/plugins/my-plugin/1.0/my-plugin-1.0.pom
 *       org/foo/plugins/my-plugin/maven-metadata.xml
 *       org/foo/plugins/maven-metadata.xml
 * </li>
 * <li>Group G contains A</li>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Path org/foo/plugins/maven-metadata.xml is requested from A</li>
 *     <li>Path org/foo/plugins/maven-metadata.xml is requested from G</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>Indy can get plugins maven-metadata.xml from A and G</li>
 * </ul>
 */
public class GroupMetadataForRemoteTest
                extends AbstractIndyFunctionalTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    private final String REMOTE = "remote-A";

    private final String GROUP = "group-G";

    private final String pomPath = "org/foo/plugins/my-plugin/1.0/my-plugin-1.0.pom";

    private final String metadataPath = "org/foo/plugins/my-plugin/maven-metadata.xml";

    private final String metadataPluginsPath = "org/foo/plugins/maven-metadata.xml";

    /* @formatter:off */
    final String metadataContent = "<metadata>"
                    + "  <groupId>org.foo.plugins</groupId>"
                    + "  <artifactId>my-plugin</artifactId>"
                    + "  <versioning>"
                    + "    <latest>1.0</latest>"
                    + "    <release>1.0</release>"
                    + "    <versions>"
                    + "      <version>1.0</version>"
                    + "    </versions>"
                    + "  </versioning>"
                    + "</metadata>";

    private final String pomContent = "<project>"
                    + "  <modelVersion>4.0.0</modelVersion>"
                    + "  <groupId>org.foo.plugins</groupId>"
                    + "  <artifactId>my-plugin</artifactId>"
                    + "  <version>1.0</version>"
                    + "</project>";

    final String metadataPluginsContent = "<metadata>"
                    + "<plugins>"
                    + "  <plugin>"
                    + "    <name>My Plugin</name>"
                    + "    <prefix>mp</prefix>"
                    + "    <artifactId>my-plugin</artifactId>"
                    + "  </plugin>"
                    + "</plugins>"
                    + "</metadata>";

    /* @formatter:on */

    @Test
    public void run() throws Exception
    {
        server.expect( server.formatUrl( REMOTE, pomPath ), 200, pomContent );
        server.expect( server.formatUrl( REMOTE, metadataPath ), 200, metadataContent );
        server.expect( server.formatUrl( REMOTE, metadataPluginsPath ), 200, metadataPluginsContent );

        RemoteRepository remote1 = new RemoteRepository( MAVEN_PKG_KEY, REMOTE, server.formatUrl( REMOTE ) );
        remote1 = client.stores().create( remote1, "remote A", RemoteRepository.class );

        Group g = new Group( MAVEN_PKG_KEY, GROUP, remote1.getKey() );
        g = client.stores().create( g, "Create group G", Group.class );

        /*// Get meta from remote
        try (final InputStream stream = client.content().get( remote1.getKey(), metadataPluginsPath ))
        {
            assertThat( stream, notNullValue() );
            String meta = IOUtils.toString( stream );
            logger.debug( "Remote A meta >>>>\n" + meta );
        }*/

        // Get meta from group
        try (final InputStream stream = client.content().get( g.getKey(), metadataPluginsPath ))
        {
            assertThat( stream, notNullValue() );
            String meta = IOUtils.toString( stream );
            logger.debug( "Group meta >>>>\n" + meta );
        }

    }
}
