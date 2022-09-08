/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Check that the group archetype is generated and checksum is created when the checksum is requested BEFORE the
 * archetype has been merged.
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>HostedRepositories A and B</li>
 *     <li>Group G with HostedRepository members A and B</li>
 *     <li>HostedRepositories A and B both contain archetype path P</li>
 *     <li>Path P has not been requested from Group G yet</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>Path P's CHECKSUM is requested from Group G</li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>Group G's archetype path P is merged from HostedRepositories A and B, and the correct checksum based on this
 *          merged archetype is returned to the user.</li>
 * </ul>
 */
public class GroupArchetypeChecksumRequestTest
        extends AbstractContentManagementTest
{
    private static final String GROUP_G = "G";
    private static final String HOSTED_A = "A";
    private static final String HOSTED_B = "B";

    private static final String ARCHETYPE_PATH = "/org/foo/bar/1.0/bar-1.0-archetype-catalog.xml";
    private static final String ARCHETYPE_CHECKSUM_PATH = ARCHETYPE_PATH + ".sha1";

    /* @formatter:off */
    private static final String ARCHETYPE_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<archetype-catalog xsi:schemaLocation=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0 http://maven.apache.org/xsd/archetype-catalog-1.0.0.xsd\"\n"
                    + "    xmlns=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0\"\n"
                    + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                    + "  <archetypes>\n"
                    + "    <archetype>\n"
                    + "      <groupId>org.jboss.fuse.fis.archetypes</groupId>\n"
                    + "      <artifactId>karaf-camel-amq-archetype</artifactId>\n"
                    + "      <version>2.2.0.fuse-sb2-7_10_0-00014</version>\n"
                    + "      <description>Karaf 4 Blueprint ActiveMQ and Camel Example</description>\n"
                    + "    </archetype>\n"
                    + "  </archetypes>\n"
                    + "</archetype-catalog>\n";
    private static final String ARCHETYPE_2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<archetype-catalog xsi:schemaLocation=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0 http://maven.apache.org/xsd/archetype-catalog-1.0.0.xsd\"\n"
                    + "    xmlns=\"http://maven.apache.org/plugins/maven-archetype-plugin/archetype-catalog/1.0.0\"\n"
                    + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                    + "  <archetypes>\n"
                    + "    <archetype>\n"
                    + "      <groupId>org.jboss.fuse.fis.archetypes</groupId>\n"
                    + "      <artifactId>karaf-camel-log-archetype</artifactId>\n"
                    + "      <version>2.2.0.fuse-sb2-7_10_0-00014</version>\n"
                    + "      <description>Karaf 4 Blueprint Camel log example</description>\n"
                    + "    </archetype>\n  "
                    + "  </archetypes>\n"
                    + "</archetype-catalog>\n";
    /* @formatter:on */

    private Group g;

    private HostedRepository a;
    private HostedRepository b;

    @Before
    public void setupRepos() throws IndyClientException
    {
        String message = "test setup";

        a = client.stores()
                  .create( new HostedRepository( MAVEN_PKG_KEY, HOSTED_A ), message, HostedRepository.class );
        b = client.stores()
                  .create( new HostedRepository( MAVEN_PKG_KEY, HOSTED_B ), message, HostedRepository.class );

        g = client.stores()
                  .create( new Group( MAVEN_PKG_KEY, GROUP_G, a.getKey(), b.getKey() ), message, Group.class );

        deployArchetypeContent( a, ARCHETYPE_PATH, ARCHETYPE_1 );
        deployArchetypeContent( b, ARCHETYPE_PATH, ARCHETYPE_2 );
    }

    protected void deployArchetypeContent( HostedRepository repo, String path, String content )
                    throws IndyClientException
    {
        client.content().store( repo.getKey(), path, new ByteArrayInputStream( content.getBytes() ) );
    }

    @Test
    public void run() throws Exception
    {
        String checksum;
        try (InputStream in = client.content().get( g.getKey(), ARCHETYPE_CHECKSUM_PATH ))
        {
            checksum = IOUtils.toString( in );
        }

        String content;
        try (InputStream in = client.content().get( g.getKey(), ARCHETYPE_PATH ))
        {
            content = IOUtils.toString( in );
        }

        // Check group G checksum is equal to one calculated from content
        assertThat( checksum, equalTo( DigestUtils.sha1Hex( content ) ) );

        // Check archetype is merged correctly
        assertThat( content, containsString( "karaf-camel-amq-archetype" ) );
        assertThat( content, containsString( "karaf-camel-log-archetype" ) );

        // Check hosted A checksum
        try (InputStream in = client.content().get( a.getKey(), ARCHETYPE_CHECKSUM_PATH ))
        {
            checksum = IOUtils.toString( in );
            assertThat( checksum, equalTo( DigestUtils.sha1Hex( ARCHETYPE_1 ) ) );
        }

        // Check hosted B checksum
        try (InputStream in = client.content().get( b.getKey(), ARCHETYPE_CHECKSUM_PATH ))
        {
            checksum = IOUtils.toString( in );
            assertThat( checksum, equalTo( DigestUtils.sha1Hex( ARCHETYPE_2 ) ) );
        }
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
