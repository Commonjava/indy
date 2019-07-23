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
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>RemoteRepositories A, B, and C, each containing metadata paths P</li>
 *     <li>Group G with A and B members</li>
 *     <li>Group G metadata path P has NOT been generated</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>User 1 GET: path P from Group G</li>
 *     <li>User 1 POST: add C to Group G membership</li>
 *     <li>User 1 GET: path P from Group G</li>
 *     <li>User 1 POST: delete C from Group G membership</li>
 *     <li>User 1 GET: path P from Group G</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>User 1 get merged content from (A, B) for the first time</li>
 *     <li>User 1 get merged content from (A, B, C) after adding C to group G</li>
 *     <li>User 1 get merged content from (A, B) after removing C from group G</li>
 * </ul>
 */
public class GroupMembershipChangeUpdateMetadataTest
        extends AbstractIndyFunctionalTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    final String path_P = "org/foo/bar/maven-metadata.xml";

    /* @formatter:off */
    final String repoContent_A = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
    final String repoContent_B = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
    final String repoContent_C = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>3.0</latest>\n" +
        "    <release>3.0</release>\n" +
        "    <versions>\n" +
        "      <version>3.0</version>\n" +
        "    </versions>\n" +
        "    <lastUpdated>20170722164334</lastUpdated>\n" +
        "  </versioning>\n" +
        "</metadata>\n";
    /* @formatter:on */

    /* @formatter:off */
    final String mergedContent_AB = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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

    /* @formatter:off */
    final String mergedContent_ABC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<metadata>\n" +
        "  <groupId>org.foo</groupId>\n" +
        "  <artifactId>bar</artifactId>\n" +
        "  <versioning>\n" +
        "    <latest>3.0</latest>\n" +
        "    <release>3.0</release>\n" +
        "    <versions>\n" +
        "      <version>1.0</version>\n" +
        "      <version>2.0</version>\n" +
        "      <version>3.0</version>\n" +
        "    </versions>\n" +
        "    <lastUpdated>20170722164334</lastUpdated>\n" +
        "  </versioning>\n" +
        "</metadata>\n";
    /* @formatter:on */

    RemoteRepository remoteRepositoryA, remoteRepositoryB, remoteRepositoryC;

    Group g;

    /**
     * Create remote repo A, B and C; register content for path P; add repo A and B to group G.
     */
    protected void prepare() throws Exception
    {
        final String repoA = "remote-A";
        final String repoB = "remote-B";
        final String repoC = "remote-C";

        final String groupName = "group-1";

        server.expect( server.formatUrl( repoA, path_P ), 200, repoContent_A );
        server.expect( server.formatUrl( repoB, path_P ), 200, repoContent_B );
        server.expect( server.formatUrl( repoC, path_P ), 200, repoContent_C );

        remoteRepositoryA = new RemoteRepository( repoA, server.formatUrl( repoA ) );
        remoteRepositoryB = new RemoteRepository( repoB, server.formatUrl( repoB ) );
        remoteRepositoryC = new RemoteRepository( repoC, server.formatUrl( repoC ) );

        client.stores().create( remoteRepositoryA, "adding remote-A", RemoteRepository.class );
        client.stores().create( remoteRepositoryB, "adding remote-B", RemoteRepository.class );
        client.stores().create( remoteRepositoryC, "adding remote-C", RemoteRepository.class );

        g = new Group( groupName, remoteRepositoryA.getKey(), remoteRepositoryB.getKey() );
        client.stores().create( g, "adding group (with repo A and B)", Group.class );
    }

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

    class GroupAddCallable
                    implements Callable<String>
    {
        private StoreKey storeKey;

        GroupAddCallable( StoreKey key )
        {
            this.storeKey = key;
        }

        @Override
        public String call() throws Exception
        {
            try
            {
                g.addConstituent( storeKey );
                client.stores().update( g, "add to group" );
            }
            catch ( IndyClientException e )
            {
                e.printStackTrace();
                return "ERROR";
            }
            return "OK";
        }
    }

    class GroupDeleteCallable
                    implements Callable<String>
    {
        private StoreKey storeKey;

        GroupDeleteCallable( StoreKey key )
        {
            this.storeKey = key;
        }

        @Override
        public String call() throws Exception
        {
            try
            {
                g.removeConstituent( storeKey );
                client.stores().update( g, "remove from group" );
            }
            catch ( IndyClientException e )
            {
                e.printStackTrace();
                return "ERROR";
            }
            return "OK";
        }
    }

    @Test
    public void run()
                    throws Exception
    {
        prepare();

        ExecutorService fixedPool = Executors.newFixedThreadPool( 1 );

        // Get merged from A, B
        Callable<String> groupMetaTask = new GroupMetaCallable( path_P );
        Future<String> user1 = fixedPool.submit( groupMetaTask );
        String metadata = user1.get();
        assertThat( metadata, equalTo( mergedContent_AB ) );

        // Add remote repo C to group
        Callable<String> groupAddTask = new GroupAddCallable( remoteRepositoryC.getKey() );
        user1 = fixedPool.submit( groupAddTask );
        String retCode = user1.get();
        assertThat( retCode, equalTo( "OK" ) );

        checkGroupMembership( remoteRepositoryA.getKey(), remoteRepositoryB.getKey(), remoteRepositoryC.getKey() );

        Thread.sleep( 2000 );

        // Get merged from A, B, C
        user1 = fixedPool.submit( groupMetaTask );
        metadata = user1.get();
        assertThat( metadata, equalTo( mergedContent_ABC ) ); // FAILS! should merge all remote repo's metadata

        // Remove remote repo C from group
        Callable<String> groupDeleteTask = new GroupDeleteCallable( remoteRepositoryC.getKey() );
        user1 = fixedPool.submit( groupDeleteTask );
        retCode = user1.get();
        assertThat( retCode, equalTo( "OK" ) );

        checkGroupMembership( remoteRepositoryA.getKey(), remoteRepositoryB.getKey() );

        // Get merged from A, B
        user1 = fixedPool.submit( groupMetaTask );
        metadata = user1.get();
        assertThat( metadata, equalTo( mergedContent_AB ) );

        fixedPool.shutdown(); // shut down
    }

    protected void checkGroupMembership( StoreKey ... keys )
                    throws Exception
    {
        List<StoreKey> members = getGroupMembers();
        assertThat( members.containsAll( Arrays.asList( keys ) ), equalTo( true ) );
    }

    protected List<StoreKey> getGroupMembers() throws Exception
    {
        Group retrieved = client.stores().listGroups().getItems().stream()
                                .filter( grp -> grp.getName().equals( g.getName() ) ).findFirst().get();
        return retrieved.getConstituents();
    }
}
