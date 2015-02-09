package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.junit.Test;

public class StoreManagement11Test
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
