package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.commonjava.aprox.client.core.helper.PathInfo;
import org.junit.Test;

public class ContentManagement01Test
    extends AbstractContentManagementTest
{

    @Test
    public void storeFileAndVerifyReturnedInfo()
        throws Exception
    {
        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";
        final PathInfo result = client.content()
                                      .store( hosted, STORE, path, stream );

        System.out.println( result );
        assertThat( result, notNullValue() );
        assertThat( result.exists(), equalTo( true ) );
    }

}
