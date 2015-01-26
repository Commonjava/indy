package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class StoreManagement03Test
    extends AbstractStoreManagementTest
{

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
}
