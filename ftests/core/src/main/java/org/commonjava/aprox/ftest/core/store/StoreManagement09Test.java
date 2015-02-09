package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class StoreManagement09Test
    extends AbstractStoreManagementTest
{

    @Test
    public void addAndModifyGroupThenRetrieveIt()
        throws Exception
    {
        final Group repo = new Group( newName() );
        client.stores()
              .create( repo, name.getMethodName(), Group.class );

        repo.setDescription( "Testing" );

        assertThat( client.stores()
                          .update( repo, name.getMethodName() ), equalTo( true ) );

        final Group result = client.stores()
                                   .load( StoreType.group, repo.getName(), Group.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
    }
}
