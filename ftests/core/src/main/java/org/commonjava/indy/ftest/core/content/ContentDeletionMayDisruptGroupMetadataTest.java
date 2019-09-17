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

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

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
 *     <li>Actions of Users 1 and 2 coordinated by Byteman to expose race condition</li>
 *     <li>User 1 GET: path P from Group G</li>
 *     <li>User 2 DELETE: remove path P from RemoteRepository A</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>User 1 request transitions lock level from WRITE to READ on path P in Group G until GET request completes</li>
 *     <li>User 2 request waits until GET request is complete, then deletes path P in RemoteRepository A,
 *     which triggers deletion of path P in Group G</li>
 * </ul>
 */
@RunWith( BMUnitRunner.class )
@BMUnitConfig( debug = true )
public class ContentDeletionMayDisruptGroupMetadataTest
        extends ContentDeletionUpdateGroupMetadataTest
{
    @BMRules( rules = {
        @BMRule(
            name = "Create Rendezvous",
            targetClass = "MavenMetadataGenerator",
            targetMethod = "<init>",
            targetLocation = "ENTRY",
            action = "createRendezvous($0, 2)" ),
        @BMRule(
            name = "Wait before generateGroupFileContent return",
            targetClass = "MavenMetadataGenerator",
            targetMethod = "generateGroupFileContent",
            targetLocation = "EXIT",
            action = "debug(\"generateGroupFileContent return waiting...\"); rendezvous($0); debug(\"generateGroupFileContent return.\")" ),
        @BMRule(
            name = "Wait before handleContentDeletion return",
            targetClass = "AbstractMergedContentGenerator",
            targetMethod = "handleContentDeletion",
            targetLocation = "EXIT",
            action = "debug(\"handleContentDeletion return waiting...\"); rendezvous($0); debug(\"handleContentDeletion return.\")" ),
    } )
    @Test
    public void run()
            throws Exception
    {
        super.prepare();

        ExecutorService fixedPool = Executors.newFixedThreadPool( 2 );

        Future<String> user1 = fixedPool.submit( groupMetaCallableTask );

        /*
         * Wait for a while for user1 to reach rendezvous.
         */
        Thread.sleep( 3000 );

        /* At this point, user1 get the meta file generated. User2 will delete the meta from RemoteRepository A which
           causes the group meta file being deleted. */

        Future<String> user2 = fixedPool.submit( deleteCallableTask );
        String retCode = user2.get();
        assertThat( retCode, equalTo( "OK" ) );

        String metadata = user1.get();
        assertThat( metadata, equalTo( null ) );

        // do it again
        user1 = fixedPool.submit( groupMetaCallableTask );
        metadata = user1.get();
        assertThat( metadata, equalTo( mergedContent ) ); // should restore the metadata. but unfortunately NFC returns null

        fixedPool.shutdown(); // shut down
    }

}
