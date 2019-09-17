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
import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.core.content.group.GroupMergeHelper;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;

import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * GIVEN:
 * <ul>
 *     <li>Hosted repos A, B and C exist</li>
 *     <li>Hosted repos A, B, and C already contain path org/foo/bar/maven-metadata.xml</li>
 *     <li>Group g</li>
 * </ul>
 *
 * WHEN:
 * <ul>
 *     <li>1. Add hostedA to g</li>
 *     <li>2. Add hostedB to g</li>
 *     <li>3. Remove hostedA from group</li>
 *     <li>4. Add HostedC to g</li>
 *     <li>5. Disable hostedB</li>
 * </ul>
 *
 * THEN:
 * <ul>
 *    <li>1. maven-metadata.xml.info for g regenerated with hostedA</li>
 *    <li>2. maven-metadata.xml.info for g regenerated with hostedA and hostedB</li>
 *    <li>3. maven-metadata.xml.info for g regenerated with hostedB</li>
 *    <li>4. maven-metadata.xml.info for g regenerated with hostedB and hostedC</li>
 *    <li>5. maven-metadata.xml.info for g regenerated with hostedC</li>
 * </ul>
 *
 */
public class GroupMetadataMergeInfoGenTest
        extends AbstractContentManagementTest
{

    @Test
    public void run()
            throws Exception
    {
        final String repoA = "hostedA";
        final String repoB = "hostedB";
        final String repoC = "hostedC";
        final String path = "org/foo/bar/maven-metadata.xml";

        /* @formatter:off */
        final String repoAContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>1.0</latest>\n" +
            "    <release>1.0</release>\n" +
            "    <versions>\n" +
            "      <version>1.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20150721164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        /* @formatter:off */
        final String repoBContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>2.0</latest>\n" +
            "    <release>2.0</release>\n" +
            "    <versions>\n" +
            "      <version>2.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20160722164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        /* @formatter:off */
        final String repoCContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<metadata>\n" +
            "  <groupId>org.foo</groupId>\n" +
            "  <artifactId>bar</artifactId>\n" +
            "  <versioning>\n" +
            "    <latest>3.0</latest>\n" +
            "    <release>3.0</release>\n" +
            "    <versions>\n" +
            "      <version>3.0</version>\n" +
            "    </versions>\n" +
            "    <lastUpdated>20160723164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
        /* @formatter:on */

        HostedRepository hostedA = createHostedAndStorePath( repoA, path, repoAContent );
        HostedRepository hostedB = createHostedAndStorePath( repoB, path, repoBContent );
        HostedRepository hostedC = createHostedAndStorePath( repoC, path, repoCContent );

        Group g = new Group( "test", hostedA.getKey() );
        g = client.stores().create( g, "adding group", Group.class );
        try (final InputStream stream = client.content().get( group, g.getName(), path ))
        {
            System.out.println( IOUtils.toString( stream ) );
        }
        assertInfoContent( g, path, "maven:hosted:hostedA\n" );

        g.addConstituent( hostedB.getKey() );
        g = updateAndRetrieve( g, "adding group", path );
        assertInfoContent( g, path, "maven:hosted:hostedA\nmaven:hosted:hostedB\n" );

        g.removeConstituent( hostedA.getKey() );
        g = updateAndRetrieve( g, "removing group", path );
        assertInfoContent( g, path, "maven:hosted:hostedB\n" );

        g.addConstituent( hostedC.getKey() );
        g = updateAndRetrieve( g, "adding group", path );
        assertInfoContent( g, path, "maven:hosted:hostedB\nmaven:hosted:hostedC\n" );

        hostedB.setDisabled( true );
        client.stores().update( hostedB, "disabled" );
        try (final InputStream stream = client.content().get( group, g.getName(), path ))
        {
            System.out.println( IOUtils.toString( stream ) );
        }
        assertInfoContent( g, path, "maven:hosted:hostedC\n" );

    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    private HostedRepository createHostedAndStorePath( final String repoName, final String path, final String content )
            throws Exception
    {
        HostedRepository hosted = new HostedRepository( repoName );
        hosted = client.stores().create( hosted, "adding " + repoName, HostedRepository.class );
        if ( StringUtils.isNotBlank( content ) )
        {
            client.content().store( hosted.getKey(), path, new ByteArrayInputStream( content.getBytes() ) );
        }
        return hosted;
    }

    private Group updateAndRetrieve( final Group g, final String changeLog, final String path )
            throws Exception
    {
        client.stores().update( g, changeLog );

        Group grp = client.stores().load( StoreType.group, g.getName(), Group.class );

        try (final InputStream stream = client.content().get( group, g.getName(), path ))
        {
            System.out.println( IOUtils.toString( stream ) );
        }
        return grp;
    }

    private void assertInfoContent( final ArtifactStore store, final String path, final String expectedContent )
            throws Exception
    {

//        final String infoFilePath =
//                String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getIndyHome(), group.name(),
//                               store.getName(), path + GroupMergeHelper.MERGEINFO_SUFFIX );
        final File infoFile = Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", store.getPackageType(),
                                         group.singularEndpointName() + "-" + store.getName(), path + GroupMergeHelper.MERGEINFO_SUFFIX ).toFile();
        assertThat( "info file doesn't exist", infoFile.exists(), equalTo( true ) );

        try (final InputStream stream = new FileInputStream( infoFile ))
        {
            System.out.println( stream );
            assertThat( IOUtils.toString( stream ), equalTo( expectedContent ) );
        }
    }

}
