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
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>RemoteRepositories A, B, and C, each containing metadata paths P</li>
 *     <li>Group G with A and B members</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Actions of Users coordinated by Byteman to expose race condition</li>
 *     <li>User 1 START</li>
 *     <li>User 1 GET: path P from Group G</li>
 *     <li>RENDEZVOUS: User 1 END <i>then</i> User 2 START</li>
 *     <li>User 2 POST: add C to Group G membership</li>
 *     <li>RENDEZVOUS: User 2 END <i>then</i> User 3 START</li>
 *     <li>User 3 GET: path P from Group G</li>
 *     <li>User 3 END</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>User 1 request gets AB content for P in G</li>
 *     <li>User 2 request deletes path P maven-metadata.xml in G</li>
 *     <li>User 3 request gets ABC content for P in G</li>
 * </ul>
 */
@RunWith( BMUnitRunner.class )
@BMUnitConfig( debug = true )
public class GroupMembershipAddMayDisruptMetadataRefreshTest
        extends GroupMembershipChangeUpdateMetadataTest
{
    /* @formatter:off */
    @BMRules( rules = {
        @BMRule(
            name = "Enable slow list processing",
            targetClass = "TestStartTrigger",
            targetMethod = "run",
            targetLocation = "EXIT",
            action = "debug(\"Setting up slow-down for list processing\"); flag(\"ready\")" ),
        @BMRule(
            name = "Slow list processing when ready",
            targetClass = "StoreChangeUtil",
            targetMethod = "listPathsAnd",
            condition = "flagged(\"ready\")",
            targetLocation = "ENTRY",
            action = "debug(\"Slowing file listing processor...\"); Thread.sleep(2000)" ),
    } )
    /* @formatter:on */
    @Test
    @Category( BytemanTest.class )
    public void run()
            throws Exception
    {
        prepare();

        logger.info("\n\n\n\n\n\nSTART Test Process\n\n\n\n");

        new TestStartTrigger().run();

        String beforeMetadata = new GroupMetaCallable( path_P ).call();

        String retCode = new GroupAddCallable( remoteRepositoryC.getKey() ).call();

        String afterMetadata = new GroupMetaCallable( path_P ).call();

        assertThat( "User 1 INCORRECT", beforeMetadata, equalTo( mergedContent_AB ) );
        assertThat( "User 2 INCORRECT", retCode, equalTo( "OK" ) );
        assertThat( "User 3 INCORRECT", afterMetadata, equalTo( mergedContent_ABC ) );
    }

    @Override
    protected void initTestConfig( final CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/threadpools.conf", "[threadpools]\nenabled=true" );
    }

    private static final class TestStartTrigger implements Runnable
    {
        public void run(){}
    }
}
