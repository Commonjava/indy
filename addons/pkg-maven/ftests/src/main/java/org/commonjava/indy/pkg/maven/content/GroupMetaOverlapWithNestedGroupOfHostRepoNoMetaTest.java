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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This test assure the multi structure group level metadata merging when added new hosted repo to a existed nested
 * group which have more repos and the repos have same metadata file. The hosted repo does not have the same meta file.
 * Cases as below: <br>
 * WHEN:
 * <ul>
 *     <li>a 3 level groups structure with nested containing layer: top -> middle -> bottom</li>
 *     <li>the bottom layer group contains two or more repos with maven-metadata.xml (eg. org/foo/bar/maven-metadata.xml)</li>
 *     <li>the top group-level merged metadata has been created by requesting this path via the top group itself</li>
 *     <li>hosted repository without the same path of meta is added to the bottom group</li>
 * </ul>
 * THEN:
 * <ul>
 *     <li>The top group's merged metadata file for specified path is NOT removed / expired after new hosted repo is added.</li>
 * </ul>
 *
 */
@Deprecated
public class GroupMetaOverlapWithNestedGroupOfHostRepoNoMetaTest
        extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    @Ignore( "Due to the new way of metadata merging with cache, this case does not apply to that and should be considered as not suitable now. See MetadataMergeListener for the new logic")
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

        // don't use the same GA as above, in case we ever decide to "actively" manage metadata in the repo manager (hint: we will)
        final String normalPath = "org/foo/other/1.0/other-1.0.pom";
        client.content()
              .store( hostedRepository.getKey(), normalPath,
                      new ByteArrayInputStream( "<version>1.0</version>".getBytes( "UTF-8" ) ) );

        final PathInfo p = client.content().getInfo( hosted, hostedRepo, normalPath );
        assertThat( "hosted content should exist", p.exists(), equalTo( true ) );

        // added hosted repo to bottom group and update
        bottomGroup.addConstituent( hostedRepository );

        client.stores().update( bottomGroup, "add new hosted" );

        System.out.printf( "\n\nUpdated group constituents are:\n  %s\n\n",
                           StringUtils.join( bottomGroup.getConstituents(), "\n  " ) );

        waitForEventPropagation();

        // the top group should not reflect the meta file deprecation and expiration
        final String gpLevelMetaFilePath =
                String.format( "%s/var/lib/indy/storage/%s/%s-%s/%s", fixture.getBootOptions().getHomeDir(),
                               MAVEN_PKG_KEY, group.name(), topGroup.getName(), path );

        assertThat( "group metadata should not be removed after merging", new File( gpLevelMetaFilePath ).exists(),
                    equalTo( true ) );

        stream = client.content().get( group, topGroup.getName(), path );

        assertThat( stream, notNullValue() );

        metadata = IOUtils.toString( stream );
        stream.close();

        assertThat( metadata, equalTo( groupContent ) );

    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
