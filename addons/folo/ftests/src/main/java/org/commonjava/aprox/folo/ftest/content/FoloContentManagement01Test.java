package org.commonjava.aprox.folo.ftest.content;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.commonjava.aprox.client.core.helper.PathInfo;
import org.commonjava.aprox.folo.client.AproxFoloContentClientModule;
import org.junit.Test;

public class FoloContentManagement01Test
    extends AbstractFoloContentManagementTest
{

    @Test
    public void storeFileAndVerifyReturnedInfo()
        throws Exception
    {
        final String trackingId = newName();
        
        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";
        final PathInfo result = client.module( AproxFoloContentClientModule.class )
                                      .store( trackingId, hosted, STORE, path, stream );

        System.out.println( result );
        assertThat( result, notNullValue() );
        assertThat( result.exists(), equalTo( true ) );
    }
}
