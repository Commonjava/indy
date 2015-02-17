package org.commonjava.aprox.subsys.http.util;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.AbstractExecutionAwareRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;

/**
 * Contains request, response, and client references, for passing raw data streams and the like back to the caller without losing track of the 
 * resources that need to be closed when the caller is finished.
 * <br/>
 * <b>NOTE:</b> This class stores the response entity {@link InputStream}, and is NOT threadsafe!
 * 
 * @author jdcasey
 *
 */
public class HttpResources
    implements Closeable
{

    private final AbstractExecutionAwareRequest request;

    private final CloseableHttpResponse response;

    private final CloseableHttpClient client;

    private InputStream responseEntityStream;

    private final AproxHttpProvider http;

    public HttpResources( final CloseableHttpClient client, final AbstractExecutionAwareRequest request,
                          final CloseableHttpResponse response, final AproxHttpProvider http )
    {
        this.client = client;
        this.request = request;
        this.response = response;
        this.http = http;
    }

    public static void cleanupResources( final HttpRequest request, final HttpResponse response,
                                         final CloseableHttpClient client, final AproxHttpProvider http )
    {
        if ( response != null && response.getEntity() != null )
        {
            EntityUtils.consumeQuietly( response.getEntity() );

            if ( response instanceof CloseableHttpResponse )
            {
                closeQuietly( (CloseableHttpResponse) response );
            }
        }

        if ( request != null )
        {
            if ( request instanceof AbstractExecutionAwareRequest )
            {
                ( (AbstractExecutionAwareRequest) request ).reset();
            }
        }

        if ( client != null )
        {
            closeQuietly( client );
        }

        http.clearRepositoryCredentials();
        http.closeConnection();
    }

    public static String entityToString( final HttpResponse response )
        throws IOException
    {
        InputStream stream = null;
        try
        {
            stream = response.getEntity()
                             .getContent();

            return IOUtils.toString( stream );
        }
        finally
        {
            closeQuietly( stream );
        }
    }

    public HttpClient getClient()
    {
        return client;
    }

    public HttpRequest getRequest()
    {
        return request;
    }

    public HttpResponse getResponse()
    {
        return response;
    }

    public HttpResourcesManagingInputStream getResponseStream()
        throws IOException
    {
        return new HttpResourcesManagingInputStream( this );
    }

    @Override
    public void close()
        throws IOException
    {
        if ( responseEntityStream != null )
        {
            // do this quietly, since it's probable that the wrapper HttpResourcesManagingInputStream will have closed it implicitly before this is called.
            IOUtils.closeQuietly( responseEntityStream );
        }

        cleanupResources( request, response, client, http );
    }

    public int getStatusCode()
    {
        return response.getStatusLine()
                       .getStatusCode();
    }

    public StatusLine getStatusLine()
    {
        return response.getStatusLine();
    }

    public InputStream getResponseEntityContent()
        throws IOException
    {
        if ( responseEntityStream == null )
        {
            responseEntityStream = response.getEntity()
                                           .getContent();
        }

        return responseEntityStream;
    }

    private static final class HttpResourcesManagingInputStream
        extends FilterInputStream
    {

        private final HttpResources resources;

        HttpResourcesManagingInputStream( final HttpResources httpResources )
            throws IOException
        {
            super( httpResources.getResponseEntityContent() );
            this.resources = httpResources;
        }

        @Override
        public void close()
            throws IOException
        {
            super.close();
            IOUtils.closeQuietly( resources );
        }

    }
}
