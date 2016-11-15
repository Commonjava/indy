/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.promote.client.IndyPromoteClientModule;
import org.commonjava.indy.promote.model.GroupPromoteRequest;
import org.commonjava.indy.promote.model.GroupPromoteResult;
import org.junit.Test;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class GroupPromoteThenHostedRepoTimeoutTest
        extends AbstractPromotionManagerTest
{

    @Test
    public void run()
            throws Exception
    {
        final int REPO_TIMEOUT_SECONDS = 6;
        final int TIMEOUT_WAITING_MILLISECONDS = 8000;
        final String hostedRepo = "hostedRepo";

        final HostedRepository repo = new HostedRepository( hostedRepo );
        repo.setRepoTimeoutSeconds( REPO_TIMEOUT_SECONDS );

        HostedRepository created = client.stores().create( repo, "adding hosted", HostedRepository.class );

        assertThat( created.getRepoTimeoutSeconds(), equalTo( REPO_TIMEOUT_SECONDS ) );

        final GroupPromoteResult result = client.module( IndyPromoteClientModule.class )
                                                .promoteToGroup(
                                                        new GroupPromoteRequest( repo.getKey(), target.getName() ) );

        HostedRepository loaded = client.stores().load( hosted, hostedRepo, HostedRepository.class );

        assertThat( result.getRequest().getSource(), equalTo( repo.getKey() ) );
        assertThat( loaded.getRepoTimeoutSeconds(), nullValue() );
        assertThat( result.getError(), nullValue() );

        // wait for timeout which is beyond the original timeout seconds, verify the original expiration schedule jobs are cancelled if no repo is deleted.
        Thread.sleep( TIMEOUT_WAITING_MILLISECONDS );
        assertThat( client.stores().exists( hosted, hostedRepo ), equalTo( true ) );
    }

    @Override
    protected ArtifactStore createTarget( String changelog )
            throws Exception
    {
        Group group = new Group( "test" );
        return client.stores().create( group, changelog, Group.class );
    }
}
