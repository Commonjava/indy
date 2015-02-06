package org.commonjava.aprox.client.core.helper;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.AbstractExecutionAwareRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.util.HttpResourcesManagingInputStream;

public class HttpResources
    implements Closeable
{

    private final AbstractExecutionAwareRequest request;

    private final CloseableHttpResponse response;

    private final CloseableHttpClient client;

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
        throws AproxClientException
    {
        return new HttpResourcesManagingInputStream( this );
    }

    @Override
    public void close()
        throws IOException
    {
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

}
