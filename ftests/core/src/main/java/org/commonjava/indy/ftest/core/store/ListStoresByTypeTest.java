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
package org.commonjava.indy.ftest.core.store;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

public class ListStoresByTypeTest
    extends AbstractStoreManagementTest
{

    @Test
    public void listByType()
        throws Exception
    {
        final Set<ArtifactStore> hosteds = new HashSet<>();
        for ( int i = 0; i < 3; i++ )
        {
            final HostedRepository repo = new HostedRepository( newName() );
            assertThat( client.stores()
                              .create( repo, name.getMethodName(), HostedRepository.class ), notNullValue() );
            hosteds.add( repo );
        }

        final Set<ArtifactStore> remotes = new HashSet<>();
        for ( int i = 0; i < 3; i++ )
        {
            final RemoteRepository repo = new RemoteRepository( newName(), newUrl() );
            assertThat( client.stores()
                              .create( repo, name.getMethodName(), RemoteRepository.class ), notNullValue() );
            remotes.add( repo );
        }

        final Set<ArtifactStore> groups = new HashSet<>();
        for ( int i = 0; i < 3; i++ )
        {
            final Group repo = new Group( newName() );
            assertThat( client.stores()
                              .create( repo, name.getMethodName(), Group.class ), notNullValue() );
            groups.add( repo );
        }

        // Now, start listing by type and verify that ONLY those of the given type are present
        checkListing( client.stores()
                            .listHostedRepositories(), hosteds,
                      Arrays.asList( remotes, groups ) );

        checkListing( client.stores()
                            .listRemoteRepositories(), remotes,
                      Arrays.asList( groups, hosteds ) );

        checkListing( client.stores()
                            .listGroups(), groups, Arrays.asList( hosteds, remotes ) );
    }

}
