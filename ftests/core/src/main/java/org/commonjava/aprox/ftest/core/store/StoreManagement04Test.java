package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.HostedRepository;
import org.junit.Test;

public class StoreManagement04Test
    extends AbstractStoreManagementTest
{

    @Test
    public void addMinimalHostedRepositoryAndRetrieveIt()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        final HostedRepository result = client.stores()
                                              .create( repo, name.getMethodName(), HostedRepository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
    }

}
