package org.commonjava.aprox.ftest.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.aprox.client.core.Aprox;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.test.fixture.core.CoreVertxServerFixture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StoreManagementTest
{

    private static Aprox client;

    public static CoreVertxServerFixture fixture = new CoreVertxServerFixture();

    @BeforeClass
    public static void start()
        throws Throwable
    {
        //        final String url = System.getProperty( "aprox.url" );
        //        if ( url == null )
        //        {
        //            throw new IllegalStateException( "Cannot read system property: 'aprox.url'." );
        //        }

        fixture.before();

        if ( !fixture.isBooted() )
        {
            throw new IllegalStateException( "server fixture failed to boot." );
        }

        client = new Aprox( fixture.getUrl() ).connect();
    }

    @AfterClass
    public static void stop()
    {
        fixture.after();
    }

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

        final RemoteRepository result = (RemoteRepository) client.stores()
                                                                 .load( StoreType.remote, rr.getName() );

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

    private String newName()
    {
        return DigestUtils.md5Hex( Long.toString( System.nanoTime() )
                                       .getBytes() );
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

        final HostedRepository result = (HostedRepository) client.stores()
                                                                 .load( StoreType.hosted, repo.getName() );

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

        final Group result = (Group) client.stores()
                                           .load( StoreType.group, repo.getName() );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
    }

}
