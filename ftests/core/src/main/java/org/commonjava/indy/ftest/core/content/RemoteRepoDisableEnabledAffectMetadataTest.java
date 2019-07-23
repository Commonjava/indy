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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.maven.galley.io.SpecialPathConstants.PKG_TYPE_MAVEN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Check that the group's metadata is deleted when member repo is disabled/enabled.
 * <br/>
 * GIVEN:
 * <ul>
 *     <li>HostedRepository A</li>
 *     <li>RemoteRepository B</li>
 *     <li>Group G with members A and B</li>
 *     <li>HostedRepository A contains a valid Path P metadata file</li>
 *     <li>RemoteRepository B contains a valid Path P metadata file</li>
 *     <li>Path P has been requested from Group G</li>
 * </ul>
 * <br/>
 * WHEN:
 * <ul>
 *     <li>B is disabled and then re-enabled</li>
 * </ul>
 * <br/>
 * THEN:
 * <ul>
 *     <li>Path P is requested from G and get metadata in A after B is disabled.</li>
 *     <li>Path P is requested from G and get metadata in A+B after B is re-enabled.</li>
 * </ul>
 */
public class RemoteRepoDisableEnabledAffectMetadataTest
        extends AbstractContentManagementTest
{

    private static final String path = "/org/foo/bar/maven-metadata.xml";

    /* @formatter:off */
    private static final String content_a = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>1.0</latest>\n" +
        "    <release>1.0</release>\n" +
        "    <versions>\n" +
        "      <version>1.0</version>\n" +
        "    </versions>\n" +
        "  </versioning>\n" +
        "</metadata>\n";

    private static final String content_b = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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

    private static final String content_ab = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>2.0</latest>\n" +
        "    <release>2.0</release>\n" +
        "    <versions>\n" +
        "      <version>1.0</version>\n" +
        "      <version>2.0</version>\n" +
        "    </versions>\n" +
        "  </versioning>\n" +
        "</metadata>\n";
    /* @formatter:on */

    private Group g;

    private HostedRepository a;

    private RemoteRepository b;

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Before
    public void setupRepos() throws Exception
    {
        String message = "test setup";

        a = client.stores().create( new HostedRepository( PKG_TYPE_MAVEN, "a" ), message, HostedRepository.class );
        deployContent( a, content_a, path );

        final String repoB = "b";
        server.expect( server.formatUrl( repoB, path ), 200, content_b );
        b = client.stores().create( new RemoteRepository( PKG_TYPE_MAVEN, repoB, server.formatUrl( repoB ) ),
                                    message, RemoteRepository.class );

        g = client.stores().create( new Group(PKG_TYPE_MAVEN, "g", a.getKey(), b.getKey() ), message, Group.class );

        String content = getContent( g, path );
        assertEquals( content_ab, content );
    }

    @Test
    public void run()
        throws Exception
    {
        // Disable B
        b.setDisabled( true );
        client.stores().update( b, "disable it" );
        /*RemoteRepository result = client.stores().load( b.getKey(), RemoteRepository.class );
        assertThat( result.isDisabled(), equalTo( true ) );*/

        String content = getContent( g, path );
        assertEquals( content_a, content );

        // Re-enable B
        b.setDisabled( false );
        client.stores().update( b, "re-enable it" );
        /*result = client.stores().load( b.getKey(), RemoteRepository.class );
        assertThat( result.isDisabled(), equalTo( false ) );*/

        content = getContent( g, path );
        assertEquals( content_ab, content );

    }

    private void deployContent( HostedRepository repo, String content, String path ) throws IndyClientException
    {
        client.content().store( repo.getKey(), path, new ByteArrayInputStream( content.getBytes() ) );
    }

    private String getContent( ArtifactStore store, String path ) throws Exception
    {
        try (final InputStream stream = client.content().get( store.getKey(), path ))
        {
            return IOUtils.toString( stream );
        }
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
