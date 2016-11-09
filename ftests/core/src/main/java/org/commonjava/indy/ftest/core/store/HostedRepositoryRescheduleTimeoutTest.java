/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.ftest.core.store;

import org.commonjava.indy.model.core.HostedRepository;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class HostedRepositoryRescheduleTimeoutTest
        extends AbstractStoreManagementTest
{
    @Test
    public void repoTimeout()
            throws Exception
    {
        final int REPO_TIMEOUT_SECONDS = 6;
        final int TIMEOUT_WAITING_MILLISECONDS = 3000;

        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "/path/to/foo.class";
        final String hostedRepo = "hostedRepo";

        HostedRepository repo = new HostedRepository( hostedRepo );
        repo.setRepoTimeoutSeconds( REPO_TIMEOUT_SECONDS );

        repo = client.stores().create( repo, "adding hosted", HostedRepository.class );

        assertThat( client.content().exists( hosted, hostedRepo, path ), equalTo( false ) );
        assertThat( client.stores().exists( hosted, hostedRepo ), equalTo( true ) );

        client.content().store( hosted, hostedRepo, path, stream );
        assertThat( client.content().exists( hosted, hostedRepo, path ), equalTo( true ) );
        assertThat( client.stores().exists( hosted, hostedRepo ), equalTo( true ) );

        // wait for first 3s
        Thread.sleep( TIMEOUT_WAITING_MILLISECONDS );

        // as the normal content re-request, the timeout interval should be re-scheduled
        client.content().get( repo.getKey(), path );

        // will wait another 3.5s
        Thread.sleep( TIMEOUT_WAITING_MILLISECONDS + 500 );
        // as rescheduled in 3s, the new timeout should be 3+6=9s, so the artifact should not be deleted
        assertThat( client.content().exists( hosted, hostedRepo, path ), equalTo( true ) );
        assertThat( client.stores().exists( hosted, hostedRepo ), equalTo( true ) );

        // another round wait for 4.5s
        Thread.sleep( TIMEOUT_WAITING_MILLISECONDS + 1500 );

        assertThat( client.content().exists( hosted, hostedRepo, path ), equalTo( false ) );
        assertThat( client.stores().exists( hosted, hostedRepo ), equalTo( false ) );
    }
}
