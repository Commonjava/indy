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

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Check that merged metadata in a group full of hosted repositories is updated when a new hosted repository is added to
 * the membership.
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>HostedRepositories A, B, and C</li>
 *     <li>Group G with HostedRepository members A and B</li>
 *     <li>HostedRepositories A, B, and C all contain metadata path P</li>
 *     <li>Each metadata file contains different versions of the same project</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>HostedRepository C is appended to the end of Group G's membership</li>
 *     <li>Metadata path P is requested from Group G <b>after events of membership change have settled</b></li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>Group G's metadata path P should reflect values in HostedRepository C's metadata path P</li>
 * </ul>
 */
public class GroupHostedMetadataRemergedOnNewMemberTest
        extends AbstractContentManagementTest
{
    private static final String GROUP_G_NAME= "G";
    private static final String HOSTED_A_NAME= "A";
    private static final String HOSTED_B_NAME= "B";
    private static final String HOSTED_C_NAME= "C";

    private static final String A_VERSION = "1.0";
    private static final String B_VERSION = "1.1";
    private static final String C_VERSION = "1.2";

    private static final String METADATA_PATH = "/org/foo/bar/maven-metadata.xml";
    private static final String POM_PATH = "/org/foo/bar/%version%/bar-%version%.pom";

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
    private static final String REPO_POM_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<project>\n" +
        "  <modelVersion>4.0.0</modelVersion>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <version>%version%</version>\n" +
        "  <packaging>pom</packaging>\n" +
        "</project>\n";
    /* @formatter:on */

    /* @formatter:off */
    private static final String BEFORE_GROUP_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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

    /* @formatter:off */
    private static final String AFTER_GROUP_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>1.2</latest>\n" +
        "    <release>1.2</release>\n" +
        "    <versions>\n" +
        "      <version>1.0</version>\n" +
        "      <version>1.1</version>\n" +
        "      <version>1.2</version>\n" +
        "    </versions>\n" +
        "    <lastUpdated>20150722164334</lastUpdated>\n" +
        "  </versioning>\n" +
        "</metadata>\n";
    /* @formatter:on */

    private Group g;

    private HostedRepository a;
    private HostedRepository b;
    private HostedRepository c;

    @Before
    public void setupRepos()
            throws IndyClientException
    {
        String message = "test setup";

        a = client.stores().create( new HostedRepository( HOSTED_A_NAME ), message, HostedRepository.class );
        b = client.stores().create( new HostedRepository( HOSTED_B_NAME ), message, HostedRepository.class );
        c = client.stores().create( new HostedRepository( HOSTED_C_NAME ), message, HostedRepository.class );

        g = client.stores().create( new Group( GROUP_G_NAME, a.getKey(), b.getKey() ), message, Group.class );

        deployContent( a, POM_PATH, REPO_POM_TEMPLATE, A_VERSION );
        deployContent( b, POM_PATH, REPO_POM_TEMPLATE, B_VERSION );
        deployContent( c, POM_PATH, REPO_POM_TEMPLATE, C_VERSION );

        deployContent( a, METADATA_PATH, REPO_METADATA_TEMPLATE, A_VERSION );
        deployContent( b, METADATA_PATH, REPO_METADATA_TEMPLATE, B_VERSION );
        deployContent( c, METADATA_PATH, REPO_METADATA_TEMPLATE, C_VERSION );
    }

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        assertContent( g, METADATA_PATH, BEFORE_GROUP_CONTENT );

        g.addConstituent( c );
        client.stores().update( g, "Adding hosted c to membership" );

        waitForEventPropagation();

        assertContent( g, METADATA_PATH, AFTER_GROUP_CONTENT );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
