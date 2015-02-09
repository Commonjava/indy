package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class StoreManagement06Test
    extends AbstractStoreManagementTest
{

    @Test
    public void addAndModifyHostedRepositoryThenRetrieveIt()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        client.stores()
              .create( repo, name.getMethodName(), HostedRepository.class );

        repo.setAllowReleases( !repo.isAllowReleases() );

        assertThat( client.stores()
                          .update( repo, name.getMethodName() ), equalTo( true ) );

        final HostedRepository result = client.stores()
                                              .load( StoreType.hosted, repo.getName(), HostedRepository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
        assertThat( result.isAllowReleases(), equalTo( repo.isAllowReleases() ) );
    }

}
