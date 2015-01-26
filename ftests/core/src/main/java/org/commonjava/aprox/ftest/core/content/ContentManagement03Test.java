package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.group;
import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreKey;
import org.junit.Test;

public class ContentManagement03Test
    extends AbstractContentManagementTest
{

    @Test
    public void storeFileInConstituentAndVerifyExistenceInGroup()
        throws Exception
    {
        final Group g = client.stores()
                              .load( group, PUBLIC, Group.class );

        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );

        assertThat( g.getConstituents()
                     .contains( new StoreKey( hosted, STORE ) ), equalTo( true ) );

        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( false ) );

        client.content()
              .store( hosted, STORE, path, stream );

        assertThat( client.content()
                          .exists( group, PUBLIC, path ), equalTo( true ) );
    }
}
