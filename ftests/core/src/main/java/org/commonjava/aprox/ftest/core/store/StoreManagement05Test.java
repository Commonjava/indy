package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class StoreManagement05Test
    extends AbstractStoreManagementTest
{

    @Test
    public void addMinimalHostedRepositoryAndDeleteIt()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        final HostedRepository result = client.stores()
                                              .create( repo, name.getMethodName(), HostedRepository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );

        client.stores()
              .delete( StoreType.hosted, repo.getName(), name.getMethodName() );

        assertThat( client.stores()
                          .exists( StoreType.hosted, repo.getName() ), equalTo( false ) );
    }

}
