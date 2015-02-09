package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.junit.Test;

public class StoreManagement10Test
    extends AbstractStoreManagementTest
{

    @Test
    public void groupAdjustsToConstituentDeletion()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        final Group group = new Group( newName() );
        group.addConstituent( repo );

        assertThat( client.stores()
                          .create( repo, name.getMethodName(), HostedRepository.class ), notNullValue() );
        assertThat( client.stores()
                          .create( group, name.getMethodName(), Group.class ), notNullValue() );

        client.stores()
              .delete( repo.getKey()
                           .getType(), repo.getName(), name.getMethodName() );

        final Group result = client.stores()
                                           .load( group.getKey()
                                               .getType(), group.getName(), Group.class );

        assertThat( result.getConstituents() == null || result.getConstituents()
                                                              .isEmpty(), equalTo( true ) );
    }
}
