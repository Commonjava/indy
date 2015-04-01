package org.commonjava.aprox.autoprox.ftest.del;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.StoreListingDTO;
import org.junit.Test;

public class DeleteGroupMemberTest
    extends AbstractAutoproxDeletionTest
{

    @Test
    public void deleteGroupConstituentRepo_RepoNotReCreatedWhenGroupMembershipAdjusted()
        throws Exception
    {
        final String named = "test";

        expectRepoAutoCreation( named );

        final RemoteRepository r = client.stores()
                                         .load( StoreType.remote, named, RemoteRepository.class );

        assertThat( r, notNullValue() );

        Group group = new Group( "group", r.getKey() );
        group = client.stores()
                      .create( group, "Adding test group", Group.class );

        client.stores()
              .delete( StoreType.remote, named, "Removing test repo" );

        System.out.println( "Waiting for server events to clear..." );
        synchronized ( this )
        {
            wait( 3000 );
        }

        final StoreListingDTO<RemoteRepository> remotes = client.stores()
                                                                .listRemoteRepositories();

        boolean found = false;
        for ( final RemoteRepository remote : remotes )
        {
            if ( remote.getName()
                       .equals( named ) )
            {
                found = true;
                break;
            }
        }

        assertThat( found, equalTo( false ) );

        group = client.stores()
                      .load( StoreType.group, group.getName(), Group.class );
        assertThat( group.getConstituents()
                         .isEmpty(), equalTo( true ) );
    }

}
