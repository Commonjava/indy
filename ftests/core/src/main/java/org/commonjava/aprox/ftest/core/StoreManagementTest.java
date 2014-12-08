package org.commonjava.aprox.ftest.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.client.core.AProx;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.junit.Before;

public class StoreManagementTest
{

    private AProx client;

    @Before
    public void startClient()
    {
        final String url = System.getProperty( "aprox.url" );
        if ( url == null )
        {
            throw new IllegalStateException( "Cannot read system property: 'aprox.url'." );
        }

        client = new AProx( url ).connect();
    }

    public void addMinimalRemoteRepositoryAndRetrieveIt()
        throws Exception
    {
        final RemoteRepository rr = new RemoteRepository( "foo", "http://www.foo.com" );
        final RemoteRepository result = client.stores()
                                              .create( rr, RemoteRepository.class );

        assertThat( result.getName(), equalTo( rr.getName() ) );
        assertThat( result.getUrl(), equalTo( rr.getUrl() ) );
        assertThat( result.equals( rr ), equalTo( true ) );
    }

}
