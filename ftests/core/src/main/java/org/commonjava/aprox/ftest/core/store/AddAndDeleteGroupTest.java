package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class AddAndDeleteGroupTest
    extends AbstractStoreManagementTest
{

    @Test
    public void addMinimalGroupThenDeleteIt()
        throws Exception
    {
        final Group repo = new Group( newName() );
        final Group result = client.stores()
                                   .create( repo, name.getMethodName(), Group.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );

        client.stores()
              .delete( StoreType.group, name.getMethodName(), repo.getName() );

        assertThat( client.stores()
                          .exists( StoreType.group, name.getMethodName() ), equalTo( false ) );
    }

}
