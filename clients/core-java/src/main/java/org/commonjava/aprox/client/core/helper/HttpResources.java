package org.commonjava.aprox.client.core.helper;

import java.io.Closeable;
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
import org.commonjava.aprox.client.core.AproxClientHttp;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.util.HttpResourcesManagingInputStream;

/**
 * Contains request, response, and client references, for passing back to a call from an {@link AproxClientModule} method. This allows the caller
 * to cleanly shutdown the associated resources after using a raw-request-style method in {@link AproxClientHttp}.
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

    public HttpResources( final CloseableHttpClient client, final AbstractExecutionAwareRequest request,
                          final CloseableHttpResponse response )
    {
        this.client = client;
        this.request = request;
        this.response = response;
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

        if ( response != null )
        {
            response.close();
        }

        if ( request != null )
        {
            request.reset();
        }

        if ( client != null )
        {
            client.close();
        }
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

}
