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
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>RemoteRepositories A and B, each containing metadata path P</li>
 *     <li>Group G with A and B members</li>
 *     <li>Group G metadata path P has NOT been generated</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>User 1 GET: path P from Group G</li>
 *     <li>User 2 DELETE: remove path P from RemoteRepository A</li>
 *     <li>User 1 GET: path P from Group G after the DELETE</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>User 1 get merged metadata.xml the first time</li>
 *     <li>User 2 get updated metadata.xml the second time where the version from RemoteRepository A is deleted</li>
 * </ul>
 */
public class ContentDeletionUpdateGroupMetadataTest
        extends AbstractIndyFunctionalTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    final String path = "org/foo/bar/maven-metadata.xml";

    final String repoA = "remote-A";
    final String repoB = "remote-B";

    final String groupName = "group-1";

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
    final String mergedContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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

    Group g;

    RemoteRepository r1, r2;

    Callable<String> groupMetaCallableTask = new Callable<String>()
    {
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
    };

    Callable<String> deleteCallableTask = new Callable<String>()
    {
        @Override
        public String call() throws Exception
        {
            try
            {
                client.content().delete( r1.getKey(), path );
            }
            catch ( IndyClientException e )
            {
                e.printStackTrace();
                return "ERROR";
            }
            return "OK";
        }
    };

    final void prepare() throws Exception
    {
        server.expect( server.formatUrl( repoA, path ), 200, repoAContent );
        server.expect( server.formatUrl( repoB, path ), 200, repoBContent );

        r1 = new RemoteRepository( repoA, server.formatUrl( repoA ) );
        r2 = new RemoteRepository( repoB, server.formatUrl( repoB ) );

        client.stores().create( r1, "adding remote-A", RemoteRepository.class );
        client.stores().create( r2, "adding remote-B", RemoteRepository.class );

        g = client.stores().create( new Group( groupName, r1.getKey(), r2.getKey() ), "adding group", Group.class );
    }

    @Test
    public void run()
            throws Exception
    {
        prepare();

        ExecutorService fixedPool = Executors.newFixedThreadPool( 1 );

        Future<String> user1 = fixedPool.submit( groupMetaCallableTask );
        String metadata = user1.get();
        assertThat( metadata, equalTo( mergedContent ) );

        Future<String> user2 = fixedPool.submit( deleteCallableTask );

        String retCode = user2.get();
        assertThat( retCode, equalTo( "OK" ) );

        server.expect( server.formatUrl( repoA, path ), 404, "Not found" );

        // do it again
        user1 = fixedPool.submit( groupMetaCallableTask );
        metadata = user1.get();
        assertThat( metadata, equalTo( repoBContent ) ); // repoA metadata is deleted

        fixedPool.shutdown(); // shut down
    }

}
