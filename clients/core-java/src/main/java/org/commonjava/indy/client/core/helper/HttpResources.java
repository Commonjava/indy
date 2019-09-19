/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.client.core.helper;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private CloseableHttpResponse response;

    private final CloseableHttpClient client;

    private InputStream responseEntityStream;

    public HttpResources( final AbstractExecutionAwareRequest request, final CloseableHttpResponse response,
                          final CloseableHttpClient client )
    {
        this.client = client;
        this.request = request;
        this.response = response;
    }

    public HttpResources( final AbstractExecutionAwareRequest request, final CloseableHttpClient client )
    {
        this.request = request;
        this.client = client;
    }

    public void setResponse( final CloseableHttpResponse response )
    {
        this.response = response;
    }

    public static void cleanupResources( final HttpRequest request, final HttpResponse response,
                                         final CloseableHttpClient client )
    {
        final Logger logger = LoggerFactory.getLogger( HttpResources.class );
        logger.info( "CLEANING UP RESOURCES via: {}", Thread.currentThread()
                                                            .getStackTrace()[2] );

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

        logger.info( "DONE: CLEANING UP RESOURCES" );
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

    public InputStream getResponseStream()
        throws IOException
    {
        return new HttpResourcesManagingInputStream( this, getResponseEntityContent() );
    }

    @Override
    public void close()
        throws IOException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Closing resources for: {}", request == null ? "UNKNOWN" : request.getRequestLine().getUri() );
        if ( responseEntityStream != null )
        {
            logger.debug( "Closing response entity stream" );
            // do this quietly, since it's probable that the wrapper HttpResourcesManagingInputStream will have closed it implicitly before this is called.
            IOUtils.closeQuietly( responseEntityStream );
        }

        cleanupResources( request, response, client );
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

    public InputStream wrapRequestStream( final InputStream stream )
    {
        return new HttpResourcesManagingInputStream( this, stream );
    }

    private static final class HttpResourcesManagingInputStream
        extends FilterInputStream
    {

        private final HttpResources resources;

        HttpResourcesManagingInputStream( final HttpResources httpResources, final InputStream stream )
        {
            super( stream );
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
