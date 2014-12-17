package org.commonjava.aprox.client.core;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.client.core.util.UrlUtils.buildUrl;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AproxClientHttp
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String baseUrl;

    private CloseableHttpClient http;

    private final AproxObjectMapper objectMapper;

    public AproxClientHttp( final String baseUrl )
    {
        this.baseUrl = baseUrl;
        this.objectMapper = new AproxObjectMapper( true );
    }

    public AproxClientHttp( final String baseUrl, final AproxObjectMapper mapper )
    {
        this.baseUrl = baseUrl;
        this.objectMapper = mapper;
    }

    public void connect()
    {
        this.http = HttpClientBuilder.create()
                                     .build();
    }

    public <T> T get( final String path, final Class<T> type )
        throws AproxClientException
    {
        checkConnected();

        final ErrorHolder holder = new ErrorHolder();
        T result = null;
        try
        {
            result = http.execute( newGet( buildUrl( baseUrl, path ) ), new ResponseHandler<T>()
            {
                @Override
                public T handleResponse( final HttpResponse response )
                    throws ClientProtocolException, IOException
                {
                    final StatusLine sl = response.getStatusLine();
                    if ( sl.getStatusCode() != 200 )
                    {
                        holder.setError( new AproxClientException(
                                                                   "Error retrieving %s from: %s. Status was: %d %s (%s)",
                                                                   type.getSimpleName(), path, sl.getStatusCode(),
                                                                   sl.getReasonPhrase(), sl.getProtocolVersion() ) );
                        return null;
                    }

                    return objectMapper.readValue( response.getEntity()
                                                           .getContent(), type );
                }
            } );
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }

        if ( holder.hasError() )
        {
            throw holder.getError();
        }

        return result;
    }

    public boolean put( final String path, final Object value )
        throws AproxClientException
    {
        return put( path, value, HttpStatus.SC_OK );
    }

    public boolean put( final String path, final Object value, final int responseCode )
        throws AproxClientException
    {
        checkConnected();

        CloseableHttpResponse response = null;
        try
        {
            final HttpPut put = newPut( buildUrl( baseUrl, path ) );

            put.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            response = http.execute( put );
            final StatusLine sl = response.getStatusLine();
            if ( sl.getStatusCode() != responseCode )
            {
                logger.error( "Error in response from: %s. Status was: %d %s (%s)", path, sl.getStatusCode(),
                              sl.getReasonPhrase(), sl.getProtocolVersion() );

                return false;
            }
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            closeQuietly( response );
        }

        return true;
    }

    public <T> T postWithResponse( final String path, final T value, final Class<T> type )
        throws AproxClientException
    {
        return postWithResponse( path, value, type, HttpStatus.SC_CREATED );
    }

    public <T> T postWithResponse( final String path, final T value, final Class<T> type, final int responseCode )
        throws AproxClientException
    {
        checkConnected();

        final ErrorHolder holder = new ErrorHolder();
        T result = null;
        try
        {
            final HttpPost post = newPost( buildUrl( baseUrl, path ) );

            post.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            result = http.execute( post, new ResponseHandler<T>()
            {
                @Override
                public T handleResponse( final HttpResponse response )
                    throws ClientProtocolException, IOException
                {
                    final StatusLine sl = response.getStatusLine();
                    if ( sl.getStatusCode() != responseCode )
                    {
                        holder.setError( new AproxClientException(
                                                                   "Error retrieving %s from: %s. Status was: %d %s (%s)",
                                                                   type.getSimpleName(), path, sl.getStatusCode(),
                                                                   sl.getReasonPhrase(), sl.getProtocolVersion() ) );
                        return null;
                    }

                    return objectMapper.readValue( response.getEntity()
                                                           .getContent(), type );
                }
            } );
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }

        if ( holder.hasError() )
        {
            throw holder.getError();
        }

        return result;
    }

    private void checkConnected()
        throws AproxClientException
    {
        if ( http == null )
        {
            throw new AproxClientException( "HTTP not connected. You must call connect() first!" );
        }
    }

    public void close()
    {
        closeQuietly( http );
    }

    public void delete( final String path )
        throws AproxClientException
    {
        delete( path, HttpStatus.SC_NO_CONTENT );
    }

    public void delete( final String path, final int responseCode )
        throws AproxClientException
    {
        checkConnected();
        
        CloseableHttpResponse response = null;
        try
        {
            final HttpDelete delete = newDelete( buildUrl( baseUrl, path ) );

            response = http.execute( delete );
            final StatusLine sl = response.getStatusLine();
            if ( sl.getStatusCode() != responseCode )
            {
                throw new AproxClientException( "Error deleting: %s. Status was: %d %s (%s)", path, sl.getStatusCode(),
                                                sl.getReasonPhrase(), sl.getProtocolVersion() );
            }
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            closeQuietly( response );
        }
    }

    public boolean exists( final String path )
        throws AproxClientException
    {
        return exists( path, HttpStatus.SC_OK );
    }

    public boolean exists( final String path, final int responseCode )
        throws AproxClientException
    {
        checkConnected();

        CloseableHttpResponse response = null;
        try
        {
            final HttpDelete delete = newDelete( buildUrl( baseUrl, path ) );

            response = http.execute( delete );
            final StatusLine sl = response.getStatusLine();
            return sl.getStatusCode() == responseCode;
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            closeQuietly( response );
        }
    }

    private static final class ErrorHolder
    {
        private AproxClientException error;

        public AproxClientException getError()
        {
            return error;
        }

        public void setError( final AproxClientException error )
        {
            this.error = error;
        }

        public boolean hasError()
        {
            return error != null;
        }

    }

    private HttpGet newGet( final String url )
    {
        final HttpGet req = new HttpGet( url );
        return req;
    }

    @SuppressWarnings( "unused" )
    private HttpHead newHead( final String url )
    {
        final HttpHead req = new HttpHead( url );
        addJsonHeaders( req );
        return req;
    }

    private void addJsonHeaders( final HttpUriRequest req )
    {
        req.addHeader( "Accept", "application/json" );
        req.addHeader( "Content-Type", "application/json" );
    }

    private HttpDelete newDelete( final String url )
    {
        final HttpDelete req = new HttpDelete( url );
        return req;
    }

    private HttpPut newPut( final String url )
    {
        final HttpPut req = new HttpPut( url );
        addJsonHeaders( req );
        return req;
    }

    private HttpPost newPost( final String url )
    {
        final HttpPost req = new HttpPost( url );
        addJsonHeaders( req );
        return req;
    }

}
