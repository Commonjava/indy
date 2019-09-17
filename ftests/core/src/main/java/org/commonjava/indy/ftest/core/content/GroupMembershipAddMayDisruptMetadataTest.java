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
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.jgroups.util.Util.assertTrue;
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
 *     <li>Actions of Users 1 and 2 coordinated by Byteman to expose race condition</li>
 *     <li>User 1 GET: path P from Group G</li>
 *     <li>User 2 POST: add C to Group G membership</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>User 1 request get null due to the addition of C, which causes metadata being deleted</li>
 *     <li>User 2 request deletes path P maven-metadata.xml</li>
 *     <li>User 1 request the second time and get the right metadata</li>
 * </ul>
 */
@RunWith( BMUnitRunner.class )
@BMUnitConfig( debug = true )
public class GroupMembershipAddMayDisruptMetadataTest
        extends GroupMembershipChangeUpdateMetadataTest
{
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
            action = "debug(\"generateGroupFileContent waiting...\"); rendezvous(\"myRendezvous\"); debug(\"generateGroupFileContent go.\")" ),
        @BMRule(
            name = "Wait after storeArtifactStore",
            targetClass = "AbstractStoreDataManager",
            targetMethod = "storeArtifactStore",
            targetLocation = "EXIT",
            action = "debug(\"storeArtifactStore waiting...\"); rendezvous(\"myRendezvous\"); debug(\"storeArtifactStore go.\")" ),
    } )
    @Test
    @Category( BytemanTest.class )
    public void run()
            throws Exception
    {
        prepare();

        ExecutorService fixedPool = Executors.newFixedThreadPool( 2 );

        Callable<String> groupMetaTask = new GroupMetaCallable( path_P );
        Future<String> user1 = fixedPool.submit( groupMetaTask );

        Thread.sleep( 3000 ); // wait for a while for user1 reach rendezvous

        Callable<String> groupAddTask = new GroupAddCallable( remoteRepositoryC.getKey() );
        Future<String> user2 = fixedPool.submit( groupAddTask );

        Thread.sleep( 2000 );

        String metadata = user1.get();
        String retCode = user2.get();

        assertThat( metadata, equalTo( null ) ); // return null due to the deletion of group metadata
        assertThat( retCode, equalTo( "OK" ) );

        // get it again and return right metadata
        user1 = fixedPool.submit( groupMetaTask );
        metadata = user1.get();
        assertThat( metadata, equalTo( mergedContent_ABC ) );

        fixedPool.shutdown(); // shut down
    }

}
