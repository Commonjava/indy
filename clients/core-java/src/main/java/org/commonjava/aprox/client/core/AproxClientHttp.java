package org.commonjava.aprox.client.core;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.aprox.client.core.util.UrlUtils.buildUrl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
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
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

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

    public Map<String, String> head( final String path )
        throws AproxClientException
    {
        return head( path, HttpStatus.SC_OK );
    }

    public Map<String, String> head( final String path, final int responseCode )
        throws AproxClientException
    {
        checkConnected();
        CloseableHttpResponse response = null;
        try
        {
            final HttpHead request = newHead( buildUrl( baseUrl, path ) );

            response = http.execute( request );
            final StatusLine sl = response.getStatusLine();
            if ( sl.getStatusCode() != responseCode )
            {
                if ( sl.getStatusCode() == HttpStatus.SC_NOT_FOUND )
                {
                    return null;
                }

                throw new AproxClientException( "Error executing HEAD: %s. Status was: %d %s (%s)", path,
                                                sl.getStatusCode(), sl.getReasonPhrase(), sl.getProtocolVersion() );
            }

            final Map<String, String> headers = new HashMap<>();
            for ( final Header header : response.getAllHeaders() )
            {
                final String name = header.getName()
                                          .toLowerCase();

                if ( !headers.containsKey( name ) )
                {
                    headers.put( name, header.getValue() );
                }
            }

            return headers;
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
                    try
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
                    finally
                    {
                        if ( response instanceof CloseableHttpResponse )
                        {
                            closeQuietly( (CloseableHttpResponse) response );
                        }
                    }
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

    public <T> T get( final String path, final TypeReference<T> typeRef )
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
                    try
                    {
                    final StatusLine sl = response.getStatusLine();
                    if ( sl.getStatusCode() != 200 )
                    {
                        holder.setError( new AproxClientException(
                                                                   "Error retrieving %s from: %s. Status was: %d %s (%s)",
                                                                   typeRef.getType(), path, sl.getStatusCode(),
                                                                   sl.getReasonPhrase(), sl.getProtocolVersion() ) );
                        return null;
                    }

                    final T value = objectMapper.readValue( response.getEntity()
                                                                    .getContent(), typeRef );

                    return value;
                    }
                    finally
                    {
                        if ( response instanceof CloseableHttpResponse )
                        {
                            closeQuietly( (CloseableHttpResponse) response );
                        }
                    }
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

    public CloseableHttpResponse getRaw( final String path )
        throws AproxClientException
    {
        return getRaw( path, Collections.singletonMap( "Accept", "*" ) );
    }

    public CloseableHttpResponse getRaw( final String path, final Map<String, String> headers )
        throws AproxClientException
    {
        checkConnected();

        CloseableHttpResponse response = null;
        try
        {
            final HttpGet req = newRawGet( buildUrl( baseUrl, path ) );

            response = http.execute( req );
            return response;
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            // DO NOT CLOSE!!!! We're handing off control of the response to the caller!
            //            closeQuietly( response );
        }
    }

    public void putWithStream( final String path, final InputStream stream )
        throws AproxClientException
    {
        putWithStream( path, stream, HttpStatus.SC_CREATED );
    }

    public void putWithStream( final String path, final InputStream stream, final int responseCode )
        throws AproxClientException
    {
        checkConnected();

        CloseableHttpResponse response = null;
        try
        {
            final HttpPut put = newPut( buildUrl( baseUrl, path ) );

            put.setEntity( new InputStreamEntity( stream ) );

            response = http.execute( put );
            final StatusLine sl = response.getStatusLine();
            if ( sl.getStatusCode() != responseCode )
            {
                throw new AproxClientException( "Error in response from: %s. Status was: %d %s (%s)", path,
                                                sl.getStatusCode(), sl.getReasonPhrase(), sl.getProtocolVersion() );
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

    public <T> T postWithResponse( final String path, final Object value, final Class<T> type )
        throws AproxClientException
    {
        return postWithResponse( path, value, type, HttpStatus.SC_CREATED );
    }

    public <T> T postWithResponse( final String path, final Object value, final Class<T> type, final int responseCode )
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
                    try
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
                    finally
                    {
                        if ( response instanceof CloseableHttpResponse )
                        {
                            closeQuietly( (CloseableHttpResponse) response );
                        }
                    }
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

    public <T> T postWithResponse( final String path, final Object value, final TypeReference<T> typeRef )
        throws AproxClientException
    {
        return postWithResponse( path, value, typeRef, HttpStatus.SC_CREATED );
    }

    public <T> T postWithResponse( final String path, final Object value, final TypeReference<T> typeRef,
                                   final int responseCode )
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
                    try
                    {
                    final StatusLine sl = response.getStatusLine();
                    if ( sl.getStatusCode() != responseCode )
                    {
                        holder.setError( new AproxClientException(
                                                                   "Error retrieving %s from: %s. Status was: %d %s (%s)",
                                                                   typeRef.getType(), path, sl.getStatusCode(),
                                                                   sl.getReasonPhrase(), sl.getProtocolVersion() ) );
                        return null;
                    }

                    return objectMapper.readValue( response.getEntity()
                                                           .getContent(), typeRef );
                    }
                    finally
                    {
                        if ( response instanceof CloseableHttpResponse )
                        {
                            closeQuietly( (CloseableHttpResponse) response );
                        }
                    }
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
            final HttpHead equest = newHead( buildUrl( baseUrl, path ) );

            response = http.execute( equest );
            final StatusLine sl = response.getStatusLine();
            if ( sl.getStatusCode() == responseCode )
            {
                return true;
            }
            else if ( sl.getStatusCode() == HttpStatus.SC_NOT_FOUND )
            {
                return false;
            }

            throw new AproxClientException( "Error checking existence of: %s. Error was: %s", path, sl );
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

    private HttpGet newRawGet( final String url )
    {
        final HttpGet req = new HttpGet( url );
        return req;
    }

    private HttpGet newGet( final String url )
    {
        final HttpGet req = new HttpGet( url );
        addJsonHeaders( req );
        return req;
    }

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

    public String getBaseUrl()
    {
        return baseUrl;
    }

}
