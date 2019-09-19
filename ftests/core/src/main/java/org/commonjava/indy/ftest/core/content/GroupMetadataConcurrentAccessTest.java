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

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.category.BytemanTest;
import org.commonjava.indy.ftest.core.fixture.CallableDelayedDownload;
import org.commonjava.indy.ftest.core.fixture.DelayedDownload;
import org.commonjava.indy.ftest.core.fixture.ReluctantInputStream;
import org.commonjava.indy.ftest.core.fixture.SlowInputStream;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A group contains one hosted repo, which contains the maven-metadata.xml in question</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Two simultaneous requests for the metadata (via the group URL)</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>Both should get the metadata.xml correctly</li>
 * </ul>
 */
@RunWith( org.jboss.byteman.contrib.bmunit.BMUnitRunner.class )
@BMUnitConfig( debug = true )
@Category( BytemanTest.class )
public class GroupMetadataConcurrentAccessTest
        extends AbstractContentManagementTest
{
    /* @formatter:off */
    final String metadataContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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

    @Test
    @BMRules( rules = {
        @BMRule( name = "init", targetClass = "org.commonjava.indy.pkg.maven.content.MavenMetadataGenerator",
                 targetMethod = "<init>",
                 targetLocation = "ENTRY",
                 action = "debug(\"Creating rendezvous\"); createRendezvous(\"wait\", 4);"),
        @BMRule( name = "wait", targetClass = "org.commonjava.indy.pkg.maven.content.MavenMetadataGenerator",
                targetMethod = "generateGroupFileContent",
                targetLocation = "ENTRY",
                action = "debug(\"Rendezvous-ing\"); rendezvous(\"wait\"); debug(\"Proceeding\")")
        }
    )

    public void run()
            throws Exception
    {
        // NOTE: MUST be coordinated with the "init" @BMRule above!
        final int threadNumber = 4;

        final String repo1 = "repo1";
        final String repo2 = "repo2";
        final String path = "org/foo/bar/maven-metadata.xml";

        RemoteRepository rr1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        rr1.setTimeoutSeconds( 30 );
        rr1 = client.stores().create( rr1, "add remote", RemoteRepository.class );

        server.expect( "GET", server.formatUrl( repo1, path ), 200,
                       new SlowInputStream( metadataContent.getBytes(), 2000 ) );

        RemoteRepository rr2 = new RemoteRepository( repo2, server.formatUrl( repo2 ) );
        rr2.setTimeoutSeconds( 30 );
        rr2 = client.stores().create( rr2, "add remote", RemoteRepository.class );

        server.expect( "GET", server.formatUrl( repo2, path ), 200,
                       new SlowInputStream( metadataContent.getBytes(), 2000 ) );

        String groupName1 = "test";
        String groupName2 = "testOuter";
        Group g1 = client.stores().create( new Group( groupName1, rr1.getKey(), rr2.getKey() ), "add group", Group.class );
        Group g2 = client.stores().create( new Group( groupName2, g1.getKey() ), "add group", Group.class );

        CountDownLatch latch = new CountDownLatch( threadNumber );
        ExecutorService exec = Executors.newCachedThreadPool();
        ExecutorCompletionService<CallableDelayedDownload> downloads = new ExecutorCompletionService<>( exec );

        for ( int i=0; i<threadNumber; i++ )
        {
            downloads.submit( new CallableDelayedDownload( client, g2.getKey(), path, 0L, latch ) );
        }

        for ( int i=0; i<threadNumber; i++)
        {
            CallableDelayedDownload download = downloads.take().get();
            assertThat( download.isMissing(), equalTo( false ) );
        }
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
