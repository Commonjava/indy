package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ContentManagement05Test
    extends AbstractContentManagementTest
{

    @Test
    public void downloadFileFromRemoteRepository()
        throws Exception
    {
        final InputStream result = client.content()
                                         .get( remote, CENTRAL, "org/commonjava/commonjava/2/commonjava-2.pom" );
        assertThat( result, notNullValue() );

        final String pom = IOUtils.toString( result );
        assertThat( pom.contains( "<groupId>org.commonjava</groupId>" ), equalTo( true ) );
    }

}
