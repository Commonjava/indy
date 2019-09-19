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
import static org.junit.Assert.assertEquals;

/**
 * <b>GIVEN:</b>
 * <li>
 *     Remote repo A contains path P (and content) for: foo/bar/0.1-SNAPSHOT/maven-metadata.xml
 * </li>
 * <li>Group G contains A</li>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Path P is requested from A</li>
 *     <li>Path P is requested from G</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>Indy can get path P from both A and G</li>
 * </ul>
 */
public class GroupMetadataForRemoteSnapshotTest
                extends AbstractIndyFunctionalTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    private final String REMOTE = "remote-A";

    private final String GROUP = "group-G";

    private final String metadataPath = "foo/bar/0.2-SNAPSHOT/maven-metadata.xml";

    /* @formatter:off */
    final String metadataContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<metadata>\n"
        + "  <groupId>foo</groupId>\n"
        + "  <artifactId>bar</artifactId>\n"
        + "  <version>0.2-SNAPSHOT</version>\n"
        + "  <versioning>\n"
        + "    <snapshot>\n"
        + "      <timestamp>20190606.173308</timestamp>\n"
        + "      <buildNumber>25</buildNumber>\n"
        + "    </snapshot>\n"
        + "    <lastUpdated>20190610015831</lastUpdated>\n"
        + "    <snapshotVersions>\n"
        + "      <snapshotVersion>\n"
        + "        <classifier>javadoc</classifier>\n"
        + "        <extension>jar</extension>\n"
        + "        <value>0.2-20190606.173308-25</value>\n"
        + "        <updated>20190606173308</updated>\n"
        + "      </snapshotVersion>\n"
        + "      <snapshotVersion>\n"
        + "        <classifier>sources</classifier>\n"
        + "        <extension>jar</extension>\n"
        + "        <value>0.2-20190606.173308-25</value>\n"
        + "        <updated>20190606173308</updated>\n"
        + "      </snapshotVersion>\n"
        + "      <snapshotVersion>\n"
        + "        <extension>jar</extension>\n"
        + "        <value>0.2-20190606.173308-25</value>\n"
        + "        <updated>20190606173308</updated>\n"
        + "      </snapshotVersion>\n"
        + "      <snapshotVersion>\n"
        + "        <extension>pom</extension>\n"
        + "        <value>0.2-20190606.173308-25</value>\n"
        + "        <updated>20190606173308</updated>\n"
        + "      </snapshotVersion>\n"
        + "    </snapshotVersions>\n"
        + "  </versioning>\n"
        + "</metadata>\n";
    /* @formatter:on */

    @Test
    public void run() throws Exception
    {
        server.expect( server.formatUrl( REMOTE, metadataPath ), 200, metadataContent );

        RemoteRepository remote1 = new RemoteRepository( MAVEN_PKG_KEY, REMOTE, server.formatUrl( REMOTE ) );
        remote1.setAllowSnapshots( true );
        remote1 = client.stores().create( remote1, "remote A", RemoteRepository.class );

        Group g = new Group( MAVEN_PKG_KEY, GROUP, remote1.getKey() );
        g = client.stores().create( g, "Create group G", Group.class );

        // Get meta from remote
        try (final InputStream stream = client.content().get( remote1.getKey(), metadataPath ))
        {
            assertThat( stream, notNullValue() );
            String meta = IOUtils.toString( stream );
            logger.debug( "Remote A meta >>>>\n" + meta );
        }

        // Get meta from group
        try (final InputStream stream = client.content().get( g.getKey(), metadataPath ))
        {
            assertThat( stream, notNullValue() );
            String meta = IOUtils.toString( stream );
            logger.debug( "Group meta >>>>\n" + meta );
            assertEquals( metadataContent, meta );
        }
    }
}
