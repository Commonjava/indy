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

public class HostedRepositoryScheduleTimeoutTest
        extends AbstractStoreManagementTest
{

    @Test
    public void repoTimeout()
            throws Exception
    {
        final int REPO_TIMEOUT_SECONDS = 6;
        final int TIMEOUT_WAITING_MILLISECONDS = 8000;

        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "/path/to/foo.class";
        final String hostedRepo = "hostedRepo";

        HostedRepository repo = new HostedRepository( hostedRepo );
        repo.setRepoTimeoutSeconds( REPO_TIMEOUT_SECONDS );

        client.stores().create( repo, "adding hosted", HostedRepository.class );

        assertThat( client.content().exists( hosted, hostedRepo, path ), equalTo( false ) );
        assertThat( client.stores().exists( hosted, hostedRepo ), equalTo( true ) );

        client.content().store( hosted, hostedRepo, path, stream );
        assertThat( client.content().exists( hosted, hostedRepo, path ), equalTo( true ) );

        // wait for 8s
        Thread.sleep( TIMEOUT_WAITING_MILLISECONDS );
        assertThat( client.content().exists( hosted, hostedRepo, path ), equalTo( false ) );
        assertThat( client.stores().exists( hosted, hostedRepo ), equalTo( false ) );
    }
}
