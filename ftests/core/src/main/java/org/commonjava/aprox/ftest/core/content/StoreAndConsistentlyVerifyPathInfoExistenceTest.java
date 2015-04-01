package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.commonjava.aprox.client.core.helper.PathInfo;
import org.junit.Test;

public class StoreAndConsistentlyVerifyPathInfoExistenceTest
    extends AbstractContentManagementTest
{

    @Test
    public void storeAndVerifyPathInfo_10Times()
        throws Exception
    {
        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";
        client.content()
              .store( hosted, STORE, path, stream );

        for ( int i = 0; i < 10; i++ )
        {
            final PathInfo result = client.content()
                                          .getInfo( hosted, STORE, path );

            assertThat( "pass: " + i + "...no result", result, notNullValue() );
            assertThat( "pass: " + i + "...doesn't exist", result.exists(), equalTo( true ) );
        }
    }

    @Override
    protected long getTestTimeoutSeconds()
    {
        return 300;
    }

}
