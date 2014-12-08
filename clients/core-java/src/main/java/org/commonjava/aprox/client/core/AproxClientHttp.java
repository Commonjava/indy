package org.commonjava.aprox.client.core;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;

public class AproxClientHttp
{
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
            result = http.execute( new HttpGet( Paths.get( baseUrl, path )
                                                     .toUri()
                                                     .toString() ), new ResponseHandler<T>()
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

    public <T> T putWithResponse( final String path, final T value, final Class<T> type )
        throws AproxClientException
    {
        return putWithResponse( path, value, type, HttpStatus.SC_CREATED );
    }

    public <T> T putWithResponse( final String path, final T value, final Class<T> type, final int responseCode )
        throws AproxClientException
    {
        checkConnected();

        final ErrorHolder holder = new ErrorHolder();
        T result = null;
        try
        {
            final HttpPut put = new HttpPut( Paths.get( baseUrl, path )
                                                  .toUri()
                                                  .toString() );

            put.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            result = http.execute( put, new ResponseHandler<T>()
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

    public <T> T postWithResponse( final String path, final T value, final Class<T> type )
        throws AproxClientException
    {
        return postWithResponse( path, value, type, HttpStatus.SC_OK );
    }

    public <T> T postWithResponse( final String path, final T value, final Class<T> type, final int responseCode )
        throws AproxClientException
    {
        checkConnected();

        final ErrorHolder holder = new ErrorHolder();
        T result = null;
        try
        {
            final HttpPost post = new HttpPost( Paths.get( baseUrl, path )
                                                     .toUri()
                                                     .toString() );

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
        delete( path, HttpStatus.SC_OK );
    }

    public void delete( final String path, final int responseCode )
        throws AproxClientException
    {
        checkConnected();
        
        final HttpDelete delete = new HttpDelete( Paths.get( baseUrl, path )
                                                     .toUri()
                                                     .toString() );
        
        CloseableHttpResponse response = null;
        try
        {
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

        final HttpDelete delete = new HttpDelete( Paths.get( baseUrl, path )
                                                       .toUri()
                                                       .toString() );

        CloseableHttpResponse response = null;
        try
        {
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

}
