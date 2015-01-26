package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class StoreManagement02Test
    extends AbstractStoreManagementTest
{

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
}
