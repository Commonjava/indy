package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.Group;
import org.junit.Test;

public class StoreManagement07Test
    extends AbstractStoreManagementTest
{

    @Test
    public void addMinimalGroupAndRetrieveIt()
        throws Exception
    {
        final Group repo = new Group( newName() );
        final Group result = client.stores()
                                   .create( repo, name.getMethodName(), Group.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
    }

}
