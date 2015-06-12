package org.commonjava.aprox.httprox;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.aprox.client.core.helper.HttpResources;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class AutoCreateRepoAndRetrievePomTest
    extends AbstractHttproxFunctionalTest
{

    private static final String USER = "user";

    private static final String PASS = "password";

    @Test
    public void run()
        throws Exception
    {
        final String testRepo = "test";
        final PomRef pom = loadPom( "simple.pom", Collections.<String, String> emptyMap() );
        final String url = server.formatUrl( testRepo, pom.path );
        server.expect( url, 200, pom.pom );

        final HttpGet get = new HttpGet( url );
        final CloseableHttpClient client = proxiedHttp();
        CloseableHttpResponse response = null;

        InputStream stream = null;
        try
        {
            response = client.execute( get, proxyContext( USER, PASS ) );
            stream = response.getEntity()
                             .getContent();
            final String resultingPom = IOUtils.toString( stream );

            assertThat( resultingPom, notNullValue() );
            assertThat( resultingPom, equalTo( pom.pom ) );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
            HttpResources.cleanupResources( get, response, client );
        }

        final RemoteRepository remoteRepo = this.client.stores()
                       .load( StoreType.remote, "httprox_127-0-0-1", RemoteRepository.class );

        assertThat( remoteRepo, notNullValue() );
        assertThat( remoteRepo.getUrl(), equalTo( server.getBaseUri() ) );
    }

}
