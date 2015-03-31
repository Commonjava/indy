package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.commonjava.aprox.client.core.helper.PathInfo;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.test.fixture.core.TestHttpServer;
import org.junit.Rule;
import org.junit.Test;

public class ContentManagement10Test
    extends AbstractContentManagementTest
{

    @Rule
    public TestHttpServer server = new TestHttpServer( "repos" );

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    @Test
    public void proxyRemoteArtifact()
        throws Exception
    {
        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );
        final String path = "org/foo/foo-project/1/foo-1.txt";
        server.expect( server.formatUrl( STORE, path ), 200, stream );

        client.stores()
              .create( new RemoteRepository( STORE, server.formatUrl( STORE ) ), "adding test proxy",
                       RemoteRepository.class );

        final PathInfo result = client.content()
                                      .getInfo( remote, STORE, path );

        assertThat( "no result", result, notNullValue() );
        assertThat( "doesn't exist", result.exists(), equalTo( true ) );
    }

}
