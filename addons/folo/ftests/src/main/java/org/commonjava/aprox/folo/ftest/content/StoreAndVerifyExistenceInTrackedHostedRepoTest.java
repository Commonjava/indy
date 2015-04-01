package org.commonjava.aprox.folo.ftest.content;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.commonjava.aprox.folo.client.AproxFoloContentClientModule;
import org.junit.Test;

public class StoreAndVerifyExistenceInTrackedHostedRepoTest
    extends AbstractFoloContentManagementTest
{

    @Test
    public void storeFileAndVerifyExistence()
        throws Exception
    {
        final String trackingId = newName();
        
        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String path = "/path/to/foo.class";

        assertThat( client.module( AproxFoloContentClientModule.class )
                          .exists( trackingId, hosted, STORE, path ), equalTo( false ) );

        client.module( AproxFoloContentClientModule.class )
              .store( trackingId, hosted, STORE, path, stream );

        assertThat( client.module( AproxFoloContentClientModule.class )
                          .exists( trackingId, hosted, STORE, path ), equalTo( true ) );
    }

}
