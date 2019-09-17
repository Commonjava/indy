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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Check that the group's merged metadata is generated then checksummed when the checksum is requested BEFORE the
 * metadata has been merged.
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>HostedRepositories A and B</li>
 *     <li>Group G with HostedRepository members A and B</li>
 *     <li>HostedRepositories A and B both contain metadata path P</li>
 *     <li>Path P has not been requested from Group G yet</li>
 *     <li>Each metadata file contains different versions of the same project</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>Path P's CHECKSUM is requested from Group G</li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>Group G's metadata path P is merged from HostedRepositories A and B, and the correct checksum based on this
 *          merged metadata is returned to the user.</li>
 *     <li>Group G's metadata path P can be retrieved and checksummed on the client side, and will match the one
 *          requested from the server.</li>
 * </ul>
 */
public class GroupMetadataChecksumRequestedFirstTest
        extends AbstractContentManagementTest
{
    private static final String GROUP_G_NAME= "G";
    private static final String HOSTED_A_NAME= "A";
    private static final String HOSTED_B_NAME= "B";

    private static final String A_VERSION = "1.0";
    private static final String B_VERSION = "1.1";

    private static final String METADATA_PATH = "/org/foo/bar/maven-metadata.xml";
    private static final String METADATA_CHECKSUM_PATH = METADATA_PATH + ".sha1";

    /* @formatter:off */
    private static final String REPO_METADATA_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>%version%</latest>\n" +
        "    <release>%version%</release>\n" +
        "    <versions>\n" +
        "      <version>%version%</version>\n" +
        "    </versions>\n" +
        "    <lastUpdated>20150722164334</lastUpdated>\n" +
        "  </versioning>\n" +
        "</metadata>\n";
    /* @formatter:on */

    /* @formatter:off */
    private static final String GROUP_METADATA_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>1.1</latest>\n" +
        "    <release>1.1</release>\n" +
        "    <versions>\n" +
        "      <version>1.0</version>\n" +
        "      <version>1.1</version>\n" +
        "    </versions>\n" +
        "    <lastUpdated>20150722164334</lastUpdated>\n" +
        "  </versioning>\n" +
        "</metadata>\n";
    /* @formatter:on */

    private Group g;

    private HostedRepository a;
    private HostedRepository b;

    @Before
    public void setupRepos()
            throws IndyClientException
    {
        String message = "test setup";

        a = client.stores().create( new HostedRepository( HOSTED_A_NAME ), message, HostedRepository.class );
        b = client.stores().create( new HostedRepository( HOSTED_B_NAME ), message, HostedRepository.class );

        g = client.stores().create( new Group( GROUP_G_NAME, a.getKey(), b.getKey() ), message, Group.class );

        deployContent( a, METADATA_PATH, REPO_METADATA_TEMPLATE, A_VERSION );
        deployContent( b, METADATA_PATH, REPO_METADATA_TEMPLATE, B_VERSION );
    }

    @Test
    public void run()
            throws Exception
    {
        String checksum;
        try(InputStream in = client.content().get( g.getKey(), METADATA_CHECKSUM_PATH))
        {
            checksum = IOUtils.toString( in );
        }

        String metadataContent = assertContent( g, METADATA_PATH, GROUP_METADATA_CONTENT );

        assertThat( checksum, equalTo( DigestUtils.shaHex( metadataContent ) ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
