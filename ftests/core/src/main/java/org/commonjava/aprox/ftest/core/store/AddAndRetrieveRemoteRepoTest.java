package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.RemoteRepository;
import org.junit.Test;

public class AddAndRetrieveRemoteRepoTest
    extends AbstractStoreManagementTest
{

    @Test
    public void addMinimalRemoteRepositoryAndRetrieveIt()
        throws Exception
    {
        final RemoteRepository rr = new RemoteRepository( newName(), "http://www.foo.com" );
        final RemoteRepository result = client.stores()
                                              .create( rr, name.getMethodName(), RemoteRepository.class );

        assertThat( result.getName(), equalTo( rr.getName() ) );
        assertThat( result.getUrl(), equalTo( rr.getUrl() ) );
        assertThat( result.equals( rr ), equalTo( true ) );
    }

}
