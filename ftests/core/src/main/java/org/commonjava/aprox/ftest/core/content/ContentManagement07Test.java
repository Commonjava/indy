package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ContentManagement07Test
    extends AbstractContentManagementTest
{

    @Test
    public void storeFileThenDownloadAndVerifyContentViaClientApi()
        throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        final InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "/path/to/foo.class";

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( false ) );

        client.content()
              .store( hosted, STORE, path, stream );

        assertThat( client.content()
                          .exists( hosted, STORE, path ), equalTo( true ) );

        final InputStream is = client.content()
                                     .get( hosted, STORE, path );
        final String result = IOUtils.toString( is );
        is.close();

        assertThat( result, equalTo( content ) );
    }
}
