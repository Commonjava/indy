package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.group;
import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;

import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreKey;
import org.junit.Test;

public class StoreTwoFilesAndVerifyPresenceInGroupTest
    extends AbstractContentManagementTest
{

    @Test
    public void storeTwoFilesInConstituentAndVerifyExistenceInGroup()
        throws Exception
    {
        final Group g = client.stores()
                              .load( group, PUBLIC, Group.class );

        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        assertThat( g.getConstituents()
                     .contains( new StoreKey( hosted, STORE ) ), equalTo( true ) );

        final String path = "/path/to/foo.txt";
        final String path2 = "/path/to/foo.txt";

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( false ) );

        assertThat( client.content()
                          .exists( hosted, STORE, path2 ), equalTo( false ) );

        client.content()
              .store( hosted, STORE, path, new ByteArrayInputStream( "This is a test".getBytes() ) );

        client.content()
              .store( hosted, STORE, path2, new ByteArrayInputStream( "This is a test".getBytes() ) );

        assertThat( client.content()
                          .exists( group, PUBLIC, path ), equalTo( true ) );

        assertThat( client.content()
                          .exists( group, PUBLIC, path2 ), equalTo( true ) );
    }
}
