package org.commonjava.aprox.ftest.core;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.StoreListingDTO;
import org.junit.Test;

public class StoreManagementTest
    extends AbstractAproxFunctionalTest
{

    @Test
    public void addMinimalRemoteRepositoryAndRetrieveIt()
        throws Exception
    {
        final RemoteRepository rr = new RemoteRepository( newName(), "http://www.foo.com" );
        final RemoteRepository result = client.stores()
                                              .create( rr, RemoteRepository.class );

        assertThat( result.getName(), equalTo( rr.getName() ) );
        assertThat( result.getUrl(), equalTo( rr.getUrl() ) );
        assertThat( result.equals( rr ), equalTo( true ) );
    }

    @Test
    public void addMinimalRemoteRepositoryAndDeleteIt()
        throws Exception
    {
        final RemoteRepository repo = new RemoteRepository( newName(), "http://www.foo.com" );
        final RemoteRepository result = client.stores()
                                              .create( repo, RemoteRepository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.getUrl(), equalTo( repo.getUrl() ) );
        assertThat( result.equals( repo ), equalTo( true ) );

        client.stores()
              .delete( StoreType.remote, repo.getName() );

        assertThat( client.stores()
                          .exists( StoreType.remote, repo.getName() ), equalTo( false ) );
    }

    @Test
    public void addAndModifyRemoteRepositoryThenRetrieveIt()
        throws Exception
    {
        final RemoteRepository rr = new RemoteRepository( newName(), "http://www.foo.com" );
        client.stores()
              .create( rr, RemoteRepository.class );

        rr.setUrl( "https://www.foo.com/" );

        assertThat( rr.getUrl(), equalTo( "https://www.foo.com/" ) );

        final boolean updated = client.stores()
                                      .update( rr );
        assertThat( updated, equalTo( true ) );

        final RemoteRepository result = client.stores()
                                              .load( StoreType.remote, rr.getName(), RemoteRepository.class );

        assertThat( result.getName(), equalTo( rr.getName() ) );
        assertThat( result.getUrl(), equalTo( rr.getUrl() ) );
        assertThat( result.equals( rr ), equalTo( true ) );
    }

    @Test
    public void addMinimalHostedRepositoryAndRetrieveIt()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        final HostedRepository result = client.stores()
                                              .create( repo, HostedRepository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
    }

    @Test
    public void addMinimalHostedRepositoryAndDeleteIt()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        final HostedRepository result = client.stores()
                                              .create( repo, HostedRepository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );

        client.stores()
              .delete( StoreType.hosted, repo.getName() );

        assertThat( client.stores()
                          .exists( StoreType.hosted, repo.getName() ), equalTo( false ) );
    }

    @Test
    public void addAndModifyHostedRepositoryThenRetrieveIt()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        client.stores()
              .create( repo, HostedRepository.class );

        repo.setAllowReleases( !repo.isAllowReleases() );

        assertThat( client.stores()
                          .update( repo ), equalTo( true ) );

        final HostedRepository result = client.stores()
                                              .load( StoreType.hosted, repo.getName(), HostedRepository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
        assertThat( result.isAllowReleases(), equalTo( repo.isAllowReleases() ) );
    }

    @Test
    public void addMinimalGroupAndRetrieveIt()
        throws Exception
    {
        final Group repo = new Group( newName() );
        final Group result = client.stores()
                                   .create( repo, Group.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
    }

    @Test
    public void addMinimalGroupThenDeleteIt()
        throws Exception
    {
        final Group repo = new Group( newName() );
        final Group result = client.stores()
                                   .create( repo, Group.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );

        client.stores()
              .delete( StoreType.group, repo.getName() );

        assertThat( client.stores()
                          .exists( StoreType.group, repo.getName() ), equalTo( false ) );
    }

    @Test
    public void addAndModifyGroupThenRetrieveIt()
        throws Exception
    {
        final Group repo = new Group( newName() );
        client.stores()
              .create( repo, Group.class );

        repo.setDescription( "Testing" );

        assertThat( client.stores()
                          .update( repo ), equalTo( true ) );

        final Group result = client.stores()
                                   .load( StoreType.group, repo.getName(), Group.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
    }

    @Test
    public void groupAdjustsToConstituentDeletion()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        final Group group = new Group( newName() );
        group.addConstituent( repo );

        assertThat( client.stores()
                          .create( repo, HostedRepository.class ), notNullValue() );
        assertThat( client.stores()
                          .create( group, Group.class ), notNullValue() );

        client.stores()
              .delete( repo.getKey()
                           .getType(), repo.getName() );

        final Group result = client.stores()
                                           .load( group.getKey()
                                               .getType(), group.getName(), Group.class );

        assertThat( result.getConstituents() == null || result.getConstituents()
                                                              .isEmpty(), equalTo( true ) );
    }

    @Test
    public void listByType()
        throws Exception
    {
        final Set<ArtifactStore> hosteds = new HashSet<>();
        for ( int i = 0; i < 3; i++ )
        {
            final HostedRepository repo = new HostedRepository( newName() );
            assertThat( client.stores()
                              .create( repo, HostedRepository.class ), notNullValue() );
            hosteds.add( repo );
        }

        final Set<ArtifactStore> remotes = new HashSet<>();
        for ( int i = 0; i < 3; i++ )
        {
            final RemoteRepository repo = new RemoteRepository( newName(), newUrl() );
            assertThat( client.stores()
                              .create( repo, RemoteRepository.class ), notNullValue() );
            remotes.add( repo );
        }

        final Set<ArtifactStore> groups = new HashSet<>();
        for ( int i = 0; i < 3; i++ )
        {
            final Group repo = new Group( newName() );
            assertThat( client.stores()
                              .create( repo, Group.class ), notNullValue() );
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

    private final void checkListing( final StoreListingDTO<? extends ArtifactStore> dto,
                                     final Set<ArtifactStore> expected, final List<Set<ArtifactStore>> banned )
    {
        final List<? extends ArtifactStore> stores = dto.getItems();

        for ( final ArtifactStore store : expected )
        {
            assertThat( store.getKey() + " should be present in:\n  " + join( keys( stores ), "\n  " ),
                        stores.contains( store ),
                        equalTo( true ) );
        }

        for ( final Set<ArtifactStore> bannedSet : banned )
        {
            for ( final ArtifactStore store : bannedSet )
            {
                assertThat( store.getKey() + " should NOT be present in:\n  " + join( keys( stores ), "\n  " ),
                            stores.contains( store ),
                            equalTo( false ) );
            }
        }
    }

    private List<StoreKey> keys( final List<? extends ArtifactStore> stores )
    {
        final List<StoreKey> keys = new ArrayList<>();
        for ( final ArtifactStore store : stores )
        {
            keys.add( store.getKey() );
        }

        return keys;
    }

}
