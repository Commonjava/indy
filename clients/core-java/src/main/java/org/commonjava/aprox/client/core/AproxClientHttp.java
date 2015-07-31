/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.client.core;

import static org.commonjava.aprox.client.core.helper.HttpResources.cleanupResources;
import static org.commonjava.aprox.client.core.helper.HttpResources.entityToString;
import static org.commonjava.aprox.client.core.util.UrlUtils.buildUrl;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.commonjava.aprox.client.core.auth.AproxClientAuthenticator;
import org.commonjava.aprox.client.core.helper.CloseBlockingConnectionManager;
import org.commonjava.aprox.client.core.helper.HttpResources;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

public class AproxClientHttp
    implements Closeable
{
    private static final int GLOBAL_MAX_CONNECTIONS = 20;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String baseUrl;

    private final AproxObjectMapper objectMapper;

    private CloseBlockingConnectionManager connectionManager;

    private HttpClientContext prototypeCtx;

    private final AproxClientAuthenticator authenticator;

    private URL url;

    public AproxClientHttp( final String baseUrl, final AproxClientAuthenticator authenticator,
                            final AproxObjectMapper mapper )
        throws AproxClientException
    {
        this.baseUrl = baseUrl;
        this.authenticator = authenticator;
        this.objectMapper = mapper;

        initPrototypeContext();
    }

    private void initPrototypeContext()
        throws AproxClientException
    {
        try
        {
            url = new URL( baseUrl );
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxClientException( "Invalid base-url: {}", e, baseUrl );
        }

        HttpClientContext ctx = HttpClientContext.create();
        if ( authenticator != null )
        {
            ctx = authenticator.decoratePrototypeContext( url, ctx );
        }

        this.prototypeCtx = ctx;
    }

    public void connect( final HttpClientConnectionManager connectionManager )
        throws AproxClientException
    {
        if ( this.connectionManager != null )
        {
            throw new AproxClientException( "Already connected! (Possibly when you called a client "
                + "API method previously.) Call close before connecting again." );
        }

        this.connectionManager = new CloseBlockingConnectionManager( connectionManager );
    }

    public synchronized void connect()
    {
        if ( this.connectionManager == null )
        {
            final PoolingHttpClientConnectionManager pcm = new PoolingHttpClientConnectionManager();
            pcm.setDefaultMaxPerRoute( GLOBAL_MAX_CONNECTIONS );

            this.connectionManager = new CloseBlockingConnectionManager( pcm );
        }
    }

    public Map<String, String> head( final String path )
        throws AproxClientException
    {
        return head( path, HttpStatus.SC_OK );
    }

    public Map<String, String> head( final String path, final int... responseCodes )
        throws AproxClientException
    {
        connect();

        HttpHead request = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;

        try
        {
            request = newJsonHead( buildUrl( baseUrl, path ) );
            client = newClient();
            response = client.execute( request );

            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                if ( sl.getStatusCode() == HttpStatus.SC_NOT_FOUND )
                {
                    return null;
                }

                throw new AproxClientException( sl.getStatusCode(), "Error executing HEAD: %s. Status was: %d %s (%s)",
                                                path, sl.getStatusCode(), sl.getReasonPhrase(), sl.getProtocolVersion() );
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
            cleanupResources( request, response, client );
        }
    }

    public <T> T get( final String path, final Class<T> type )
        throws AproxClientException
    {
        connect();

        HttpGet request = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            request = newJsonGet( buildUrl( baseUrl, path ) );
            response = client.execute( request );

            final StatusLine sl = response.getStatusLine();
            if ( sl.getStatusCode() != 200 )
            {
                if ( sl.getStatusCode() == 404 )
                {
                    return null;
                }

                throw new AproxClientException( sl.getStatusCode(), "Error retrieving %s from: %s.\n%s",
                                                type.getSimpleName(), path, new AproxResponseErrorDetails( response ) );
            }

            final String json = entityToString( response );
            logger.info( "Got JSON:\n\n{}\n\n", json );
            final T value = objectMapper.readValue( json, type );

            logger.debug( "Got result object: {}", value );

            return value;
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( request, response, client );
        }
    }

    public <T> T get( final String path, final TypeReference<T> typeRef )
        throws AproxClientException
    {
        connect();

        HttpGet request = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            request = newJsonGet( buildUrl( baseUrl, path ) );
            response = client.execute( request );
            final StatusLine sl = response.getStatusLine();
            if ( sl.getStatusCode() != 200 )
            {
                if ( sl.getStatusCode() == 404 )
                {
                    return null;
                }

                throw new AproxClientException( sl.getStatusCode(), "Error retrieving %s from: %s.\n%s",
                                                typeRef.getType(), path, new AproxResponseErrorDetails( response ) );
            }

            final String json = entityToString( response );
            final T value = objectMapper.readValue( json, typeRef );

            return value;
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( request, response, client );
        }
    }

    public HttpResources getRaw( final HttpGet req )
        throws AproxClientException
    {
        connect();

        CloseableHttpResponse response = null;
        try
        {
            final CloseableHttpClient client = newClient();

            response = client.execute( req );
            return new HttpResources( req, response, client );
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

    public HttpResources getRaw( final String path )
        throws AproxClientException
    {
        return getRaw( path, Collections.singletonMap( "Accept", "*" ) );
    }

    public HttpResources getRaw( final String path, final Map<String, String> headers )
        throws AproxClientException
    {
        connect();

        CloseableHttpResponse response = null;
        try
        {
            final HttpGet req = newRawGet( buildUrl( baseUrl, path ) );
            final CloseableHttpClient client = newClient();

            response = client.execute( req );
            return new HttpResources( req, response, client );
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

    public void putWithStream( final String path, final InputStream stream, final int... responseCodes )
        throws AproxClientException
    {
        connect();

        final HttpPut put = newRawPut( buildUrl( baseUrl, path ) );
        final CloseableHttpClient client = newClient();
        CloseableHttpResponse response = null;
        try
        {
            put.setEntity( new InputStreamEntity( stream ) );

            response = client.execute( put );
            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new ClientProtocolException( new AproxClientException( sl.getStatusCode(),
                                                                             "Error in response from: %s.\n%s", path,
                                                                             new AproxResponseErrorDetails( response ) ) );
            }

        }
        catch ( final ClientProtocolException e )
        {
            final Throwable cause = e.getCause();
            if ( cause != null && ( cause instanceof AproxClientException ) )
            {
                throw (AproxClientException) cause;
            }

            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( put, response, client );
        }
    }

    public boolean put( final String path, final Object value )
        throws AproxClientException
    {
        return put( path, value, HttpStatus.SC_OK, HttpStatus.SC_CREATED );
    }

    public boolean put( final String path, final Object value, final int... responseCodes )
        throws AproxClientException
    {
        connect();

        HttpPut put = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            put = newJsonPut( buildUrl( baseUrl, path ) );

            put.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            response = client.execute( put );
            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new AproxClientException( sl.getStatusCode(), "Error in response from: %s.\n%s", path,
                                                new AproxResponseErrorDetails( response ) );
            }
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( put, response, client );
        }

        return true;
    }

    public <T> T postWithResponse( final String path, final Object value, final Class<T> type )
        throws AproxClientException
    {
        return postWithResponse( path, value, type, HttpStatus.SC_CREATED );
    }

    public <T> T postWithResponse( final String path, final Object value, final Class<T> type,
                                   final int... responseCodes )
        throws AproxClientException
    {
        connect();

        HttpPost post = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            post = newJsonPost( buildUrl( baseUrl, path ) );

            post.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            response = client.execute( post );

            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new AproxClientException( sl.getStatusCode(), "Error retrieving %s from: %s.\n%s",
                                                type.getSimpleName(), path, new AproxResponseErrorDetails( response ) );
            }

            final String json = entityToString( response );
            return objectMapper.readValue( json, type );
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( post, response, client );
        }
    }

    public boolean validResponseCode( final int statusCode, final int[] responseCodes )
    {
        for ( final int code : responseCodes )
        {
            if ( code == statusCode )
            {
                return true;
            }
        }
        return false;
    }

    public <T> T postWithResponse( final String path, final Object value, final TypeReference<T> typeRef )
        throws AproxClientException
    {
        return postWithResponse( path, value, typeRef, HttpStatus.SC_CREATED );
    }

    public <T> T postWithResponse( final String path, final Object value, final TypeReference<T> typeRef,
                                   final int... responseCodes )
        throws AproxClientException
    {
        connect();

        HttpPost post = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            post = newJsonPost( buildUrl( baseUrl, path ) );

            post.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            response = client.execute( post );

            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new AproxClientException( sl.getStatusCode(), "Error retrieving %s from: %s.\n%s",
                                                typeRef.getType(), path, new AproxResponseErrorDetails( response ) );
            }

            final String json = entityToString( response );
            return objectMapper.readValue( json, typeRef );
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( post, response, client );
        }
    }

    @Override
    public void close()
    {
        logger.info( "Shutting down aprox client HTTP manager" );
        connectionManager.reallyShutdown();
    }

    public void delete( final String path )
        throws AproxClientException
    {
        delete( path, HttpStatus.SC_NO_CONTENT );
    }

    public void delete( final String path, final int... responseCodes )
        throws AproxClientException
    {
        connect();

        HttpDelete delete = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            delete = newDelete( buildUrl( baseUrl, path ) );

            response = client.execute( delete );
            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new AproxClientException( sl.getStatusCode(), "Error deleting: %s.\n%s", path,
                                                new AproxResponseErrorDetails( response ) );
            }
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( delete, response, client );
        }
    }

    public void deleteWithChangelog( final String path, final String changelog )
        throws AproxClientException
    {
        deleteWithChangelog( path, changelog, HttpStatus.SC_NO_CONTENT );
    }

    public void deleteWithChangelog( final String path, final String changelog, final int... responseCodes )
        throws AproxClientException
    {
        connect();

        HttpDelete delete = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            delete = newDelete( buildUrl( baseUrl, path ) );
            delete.setHeader( ArtifactStore.METADATA_CHANGELOG, changelog );

            response = client.execute( delete );
            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new AproxClientException( sl.getStatusCode(), "Error deleting: %s.\n%s", path,
                                                new AproxResponseErrorDetails( response ) );
            }
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( delete, response, client );
        }
    }

    public boolean exists( final String path )
        throws AproxClientException
    {
        return exists( path, HttpStatus.SC_OK );
    }

    public boolean exists( final String path, final int... responseCodes )
        throws AproxClientException
    {
        connect();

        HttpHead request = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            request = newJsonHead( buildUrl( baseUrl, path ) );

            response = client.execute( request );
            final StatusLine sl = response.getStatusLine();
            if ( validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                return true;
            }
            else if ( sl.getStatusCode() == HttpStatus.SC_NOT_FOUND )
            {
                return false;
            }

            throw new AproxClientException( sl.getStatusCode(), "Error checking existence of: %s.\n%s", path,
                                            new AproxResponseErrorDetails( response ) );
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "AProx request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( request, response, client );
        }
    }

    public void cleanup( final HttpRequest request, final HttpResponse response, final CloseableHttpClient client )
    {
        cleanupResources( request, response, client );
    }

    public String toAproxUrl( final String... path )
    {
        return buildUrl( baseUrl, path );
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public CloseableHttpClient newClient()
        throws AproxClientException
    {
        HttpClientBuilder builder = HttpClients.custom()
                                               .setConnectionManager( connectionManager );

        if ( authenticator != null )
        {
            builder = authenticator.decorateClientBuilder( url, builder );
        }
        return builder.build();
    }

    public HttpClientContext newContext()
    {
        return new HttpClientContext( prototypeCtx );
    }

    public HttpGet newRawGet( final String url )
    {
        final HttpGet req = new HttpGet( url );
        return req;
    }

    public HttpGet newJsonGet( final String url )
    {
        final HttpGet req = new HttpGet( url );
        addJsonHeaders( req );
        return req;
    }

    public HttpHead newJsonHead( final String url )
    {
        final HttpHead req = new HttpHead( url );
        addJsonHeaders( req );
        return req;
    }

    public void addJsonHeaders( final HttpUriRequest req )
    {
        req.addHeader( "Accept", "application/json" );
        req.addHeader( "Content-Type", "application/json" );
    }

    public HttpDelete newDelete( final String url )
    {
        final HttpDelete req = new HttpDelete( url );
        return req;
    }

    public HttpPut newJsonPut( final String url )
    {
        final HttpPut req = new HttpPut( url );
        addJsonHeaders( req );
        return req;
    }

    public HttpPut newRawPut( final String url )
    {
        final HttpPut req = new HttpPut( url );
        return req;
    }

    public HttpPost newJsonPost( final String url )
    {
        final HttpPost req = new HttpPost( url );
        addJsonHeaders( req );
        return req;
    }

    public AproxObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

}
