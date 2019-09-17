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

import org.commonjava.indy.ftest.core.category.BytemanTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

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
 *     <li>RemoteRepositories A and B, each containing metadata paths P</li>
 *     <li>Group G with A and B members</li>
 *     <li>Group G metadata path P has been generated</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Actions of Users 1 and 2 coordinated by Byteman to expose race condition</li>
 *     <li>User 1 GET: path P from Group G</li>
 *     <li>User 2 DELETE: remove repository B</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>User 1 request get null due to the deletion of B, which causes metadata being deleted</li>
 *     <li>User 2 deletes path P metadata.xml from Group G</li>
 *     <li>User 1 request the second time and get the right metadata</li>
 * </ul>
 */
@RunWith( BMUnitRunner.class )
@BMUnitConfig( debug = true )
public class GroupMembershipDeletionMayDisruptMetadataTest
        extends GroupMembershipChangeUpdateMetadataTest
{
    /*
     * This flag is used to indicate the path_P has been generated.
     * This is different from GroupMembershipAddMayDisruptMetadataTest to cover another case where the group metadata
     * has been generated but GET request can still get null due to group membership change.
     */
    static volatile boolean prepareDone = false;

    public static boolean isPrepareDone()
    {
        return prepareDone;
    }

    @BMRules( rules = {
        @BMRule(
            name = "Create Rendezvous",
            targetClass = "MavenMetadataGenerator",
            targetMethod = "<init>",
            targetLocation = "ENTRY",
            action = "createRendezvous(\"myRendezvous\", 2)" ),
        @BMRule(
            name = "Wait after generateGroupFileContent",
            targetClass = "MavenMetadataGenerator",
            targetMethod = "generateGroupFileContent",
            targetLocation = "EXIT",
            condition = "org.commonjava.indy.ftest.core.content.GroupMembershipDeletionMayDisruptMetadataTest.isPrepareDone()",
            action = "debug(\"generateGroupFileContent waiting...\"); rendezvous(\"myRendezvous\"); debug(\"generateGroupFileContent go.\")" ),
        @BMRule(
            name = "Wait after storeArtifactStore",
            targetClass = "AbstractStoreDataManager",
            targetMethod = "storeArtifactStore",
            targetLocation = "EXIT",
            condition = "org.commonjava.indy.ftest.core.content.GroupMembershipDeletionMayDisruptMetadataTest.isPrepareDone()",
            action = "debug(\"storeArtifactStore waiting...\"); rendezvous(\"myRendezvous\"); debug(\"storeArtifactStore go.\")" ),
    } )
    @Test
    @Category( BytemanTest.class )
    public void run()
            throws Exception
    {
        prepare();

        ExecutorService fixedPool = Executors.newFixedThreadPool( 2 );

        // Send request for path_P to force group metadata generation
        GroupMetaCallable groupMetaTask = new GroupMetaCallable( path_P );
        Future<String> user1 = fixedPool.submit( groupMetaTask );
        user1.get();

        prepareDone = true;

        // request for path_P again, this time rendezvous come in
        user1 = fixedPool.submit( groupMetaTask );

        waitForEventPropagation();

        Callable<String> deleteTask = new GroupDeleteCallable( remoteRepositoryB.getKey() );
        Future<String> user2 = fixedPool.submit( deleteTask );

        String metadata = user1.get();
        String retCode = user2.get();

        assertThat( metadata, equalTo( null ) ); // return null due to the deletion of B
        assertThat( retCode, equalTo( "OK" ) );

        // check remoteB is deleted
        List<StoreKey> l = getGroupMembers();
        assertThat( l.size(), equalTo( 1 ) );

        // get it again and return right metadata
        user1 = fixedPool.submit( groupMetaTask );
        metadata = user1.get();
        assertThat( metadata, equalTo( repoContent_A ) );

        fixedPool.shutdown(); // shut down
    }

    @Override
    protected int getTestTimeoutMultiplier()
    {
        return 3;
    }

}
