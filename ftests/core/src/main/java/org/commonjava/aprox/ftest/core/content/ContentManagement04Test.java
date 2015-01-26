package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ContentManagement04Test
    extends AbstractContentManagementTest
{

    @Test
    public void storeAndRetrieveFile()
        throws Exception
    {
        final byte[] bytes = ( "This is a test: " + System.nanoTime() ).getBytes();
        final InputStream stream = new ByteArrayInputStream( bytes );

        final String path = "/path/to/foo.class";
        client.content()
              .store( hosted, STORE, path, stream );

        final InputStream result = client.content()
                                         .get( hosted, STORE, path );
        final byte[] resultBytes = IOUtils.toByteArray( result );

        assertThat( Arrays.equals( bytes, resultBytes ), equalTo( true ) );
    }
}
