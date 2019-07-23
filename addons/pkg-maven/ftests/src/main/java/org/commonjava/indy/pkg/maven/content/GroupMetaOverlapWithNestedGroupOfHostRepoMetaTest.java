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
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This test assure the multi structure group level metadata merging when added new hosted repo to a existed nested group which have more repos
 * and the repos have same metadata file. The hosted repo also has the same meta file. cases as below: <br>
 * WHEN:
 * <ul>
 *     <li>a 3 level groups structure with nested containing layer: top -> middle -> bottom</li>
 *     <li>the bottom layer group contains two or more repos with maven-metadata.xml (eg. org/foo/bar/maven-metadata.xml)</li>
 *     <li>the top group-level merged metadata has been created by requesting this path via the top group itself</li>
 *     <li>hosted repository with the same path of meta is added to the bottom group</li>
 * </ul>
 * THEN:
 * <ul>
 *     <li>the top group's merged metadata file should be deleted / de-indexed</li>
       <li>re-requesting this metadata path via the top group should reflect versions in the new hosted repo</li>
 * </ul>
 *
 */
public class GroupMetaOverlapWithNestedGroupOfHostRepoMetaTest
        extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    @Category( EventDependent.class )
    public void run()
            throws Exception
    {
        final String repo1 = "repo1";
        final String repo2 = "repo2";
        final String path = "org/foo/bar/maven-metadata.xml";

        /* @formatter:off */
        final String repo1Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>1.0</latest>\n" +
            "    <release>1.0</release>\n" +
            "    <versions>\n" +
            "      <version>1.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20150722164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        /* @formatter:off */
        final String repo2Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>1.1</latest>\n" +
            "    <release>1.1</release>\n" +
            "    <versions>\n" +
            "      <version>1.1</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20150822164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        server.expect( server.formatUrl( repo1, path ), 200, repo1Content );
        server.expect( server.formatUrl( repo2, path ), 200, repo2Content );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        remote1 = client.stores().create( remote1, "adding remote", RemoteRepository.class );

        RemoteRepository remote2 = new RemoteRepository( repo2, server.formatUrl( repo2 ) );
        remote2 = client.stores().create( remote2, "adding remote", RemoteRepository.class );

        // constructs 3 level group structure: top -> middle -> bottom
        // and the bottom group contains repo1 and repo2
        Group bottomGroup = new Group( "bottom", remote1.getKey(), remote2.getKey() );
        bottomGroup = client.stores().create( bottomGroup, "adding bottom group", Group.class );
        System.out.printf( "\n\nBottom group constituents are:\n  %s\n\n", StringUtils.join( bottomGroup.getConstituents(), "\n  " ) );

        Group middleGroup = new Group( "middle", bottomGroup.getKey() );
        middleGroup = client.stores().create( middleGroup, "adding middle group", Group.class );
        System.out.printf( "\n\nMiddle group constituents are:\n  %s\n\n", StringUtils.join( middleGroup.getConstituents(), "\n  " ) );

        Group topGroup = new Group( "top", middleGroup.getKey() );
        topGroup = client.stores().create( topGroup, "adding top group", Group.class );
        System.out.printf( "\n\nTop group constituents are:\n  %s\n\n",
                           StringUtils.join( topGroup.getConstituents(), "\n  " ) );

        InputStream stream = client.content().get( group, topGroup.getName(), path );
        assertThat( stream, notNullValue() );

        String metadata = IOUtils.toString( stream );
        stream.close();

        /* @formatter:off */
        final String groupContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
            "    <lastUpdated>20150822164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */
        assertThat( metadata, equalTo( groupContent ) );

        final String hostedRepo = "hostedRepo";
        HostedRepository hostedRepository = new HostedRepository( hostedRepo );
        hostedRepository = client.stores().create( hostedRepository, "adding hosted", HostedRepository.class );

        /* @formatter:off */
        final String hostedMetaContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>1.2</latest>\n" +
            "    <release>1.2</release>\n" +
            "    <versions>\n" +
            "      <version>1.2</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20150922164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        client.content()
              .store( hostedRepository.getKey(), path,
                      new ByteArrayInputStream( hostedMetaContent.getBytes( "UTF-8" ) ) );

        final PathInfo p = client.content().getInfo( hosted, hostedRepo, path );
        assertThat( "hosted metadata should exist", p.exists(), equalTo( true ) );

        // added hosted repo to bottom group and update
        bottomGroup.addConstituent( hostedRepository );

        client.stores().update( bottomGroup, "add new hosted to bottom" );

        System.out.printf( "\n\nUpdated bottom group constituents are:\n  %s\n\n",
                           StringUtils.join( bottomGroup.getConstituents(), "\n  " ) );

        waitForEventPropagation();

        // the top group should reflect the meta file deprecation and re-indexing
        final String gpLevelMetaFilePath =
                String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getHomeDir(), group.name(),
                               topGroup.getName(), path );
        assertThat( "group metadata should be removed after merging", new File( gpLevelMetaFilePath ).exists(),
                    equalTo( false ) );

        stream = client.content().get( group, topGroup.getName(), path );
        assertThat( stream, notNullValue() );

        metadata = IOUtils.toString( stream );
        stream.close();

        /* @formatter:off */
        final String updGroupContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
            "    <lastUpdated>20150922164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */
        assertThat( metadata, equalTo( updGroupContent ) );

    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
