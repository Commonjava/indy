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
package org.commonjava.indy.promote.ftest;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.ftest.core.category.BytemanTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.PathsPromoteRequest;
import org.commonjava.indy.promote.model.PathsPromoteResult;
import org.commonjava.test.http.expect.ExpectationServer;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>RemoteRepositories A and B, each containing metadata path P</li>
 *     <li>HostedRepository C which does NOT contain path P</li>
 *     <li>Group G with A and B members</li>
 *     <li>Group G metadata path P has NOT been generated</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Actions of Users 1 and 2 coordinated by Byteman to expose race condition</li>
 *     <li>User 1 GET: path P from Group G</li>
 *     <li>User 2 POST: by-path promotion of path P from RemoteRepository A to HostedRepository C with purge option enabled</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>User 1 request get null the first time due to the deletion of path P metadata by User 2</li>
 *     <li>User 2 deletes path P in RemoteRepository A, which triggers deletion of path P in Group G</li>
 *     <li>User 1 request get path P metadata the second time successfully (merged from A and B)</li>
 * </ul>
 */
@RunWith( org.jboss.byteman.contrib.bmunit.BMUnitRunner.class )
@BMUnitConfig( debug = true )
@Category( BytemanTest.class )
public class ByPathPromotionWithPurgeMayDisruptMetadataTest
        extends AbstractIndyFunctionalTest
{
    /* @formatter:off */
    final String metadataContentA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
    final String metadataContentB = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
    final String metadataContent_AB = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
            "    <lastUpdated>20160722164334</lastUpdated>\n" +
            "  </versioning>\n" +
            "</metadata>\n";
    /* @formatter:on */

    class GroupMetaCallable
                    implements Callable<String>
    {
        private String path;

        GroupMetaCallable( String path )
        {
            this.path = path;
        }

        @Override
        public String call() throws Exception
        {
            try (InputStream in = client.content().get( g.getKey(), path ))
            {
                if ( in != null )
                {
                    return IOUtils.toString( in );
                }
                return null;
            }
        }
    }

    class PromotionCallable
                    implements Callable<PathsPromoteResult>
    {
        private String path;

        private StoreKey source, target;

        PromotionCallable( StoreKey source, StoreKey target, String path )
        {
            this.source = source;
            this.target = target;
            this.path = path;
        }

        @Override
        public PathsPromoteResult call() throws Exception
        {
            try
            {
                return client.module( IndyPromoteClientModule.class )
                             .promoteByPath( new PathsPromoteRequest( source, target, path ).setPurgeSource( true ) );
            }
            catch ( IndyClientException e )
            {
                e.printStackTrace();
                return null;
            }
        }
    }

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    Group g;

    @Test
    @BMRules( rules = {
        @BMRule( name = "init", targetClass = "org.commonjava.indy.pkg.maven.content.MavenMetadataGenerator",
                 targetMethod = "start",
                 targetLocation = "ENTRY",
                 action = "debug(\"Creating rendezvous ...\"); createRendezvous(\"wait\", 2);"),
        @BMRule( name = "wait_1", targetClass = "org.commonjava.indy.pkg.maven.content.MavenMetadataGenerator",
                targetMethod = "generateGroupFileContent",
                targetLocation = "EXIT",
                action = "debug(\"Rendezvous-ing\"); rendezvous(\"wait\"); debug(\"Proceeding\")"),
        @BMRule( name = "wait_2", targetClass = "org.commonjava.indy.promote.data.PromotionManager",
                targetMethod = "promotePaths",
                targetLocation = "EXIT",
                action = "debug(\"Rendezvous-ing\"); rendezvous(\"wait\"); debug(\"Proceeding\")")
        }
    )
    public void run()
            throws Exception
    {
        final String repo1 = "repo1";
        final String repo2 = "repo2";
        final String path = "org/foo/bar/maven-metadata.xml";

        RemoteRepository rr1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
        rr1.setTimeoutSeconds( 30 );
        rr1 = client.stores().create( rr1, "add remote A", RemoteRepository.class );

        server.expect( "GET", server.formatUrl( repo1, path ), 200, metadataContentA );

        RemoteRepository rr2 = new RemoteRepository( repo2, server.formatUrl( repo2 ) );
        rr2.setTimeoutSeconds( 30 );
        rr2 = client.stores().create( rr2, "add remote B", RemoteRepository.class );

        server.expect( "GET", server.formatUrl( repo2, path ), 200, metadataContentB );

        String groupName1 = "test";
        g = client.stores().create( new Group( groupName1, rr1.getKey(), rr2.getKey() ), "add group", Group.class );

        HostedRepository hosted = new HostedRepository( "hosted1" );
        client.stores().create( hosted, "add hosted repo C", HostedRepository.class );


        ExecutorService fixedPool = Executors.newFixedThreadPool( 2 );

        Future<String> downloadTask = fixedPool.submit( new GroupMetaCallable( path ) );

        Thread.sleep( 3000 ); // wait for a while for user 1 to reach MavenMetadataGenerator

        Future<PathsPromoteResult> promotionTask = fixedPool.submit( new PromotionCallable( rr1.getKey(), hosted.getKey(), path ) );

        // get null the first time due to the deletion of path P metadata
        String metadata = downloadTask.get();
        assertThat( metadata, nullValue() );

        // check promotion status
        PathsPromoteResult result = promotionTask.get();
        assertThat( result.getError(), nullValue() );

        // get path P metadata the second time
        downloadTask = fixedPool.submit( new GroupMetaCallable( path ) );
        metadata = downloadTask.get();
        assertEquals( metadataContent_AB, metadata );

    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new IndyPromoteClientModule() );
    }

}
