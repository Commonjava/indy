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
package org.commonjava.indy.client.core;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang.StringUtils;
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
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.VersionInfo;
import org.commonjava.indy.client.core.auth.IndyClientAuthenticator;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.inject.IndyVersioningProvider;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.util.jhttpc.HttpFactory;
import org.commonjava.util.jhttpc.JHttpCException;
import org.commonjava.util.jhttpc.auth.PasswordManager;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.commonjava.indy.IndyContentConstants.CHECK_CACHE_ONLY;
import static org.commonjava.indy.client.core.helper.HttpResources.cleanupResources;
import static org.commonjava.indy.client.core.helper.HttpResources.entityToString;
import static org.commonjava.indy.client.core.util.UrlUtils.buildUrl;
import static org.commonjava.indy.stats.IndyVersioning.HEADER_INDY_API_VERSION;

public class IndyClientHttp
        implements Closeable
{
    public static final int GLOBAL_MAX_CONNECTIONS = 20;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final IndyObjectMapper objectMapper;

    private final SiteConfig location;

    private final HttpFactory factory;

    private final String baseUrl;

    private List<Header> defaultHeaders;

    private Map<String, String> mdcCopyMappings = new HashMap<>();

    /**
     *
     * @param authenticator
     * @param mapper
     * @param location
     * @param apiVersion
     * @param mdcCopyMappings a map of fields to copy from LoggingMDC to http request headers where key=MDCMey and value=headerName
     * @throws IndyClientException
     */
    public IndyClientHttp( final IndyClientAuthenticator authenticator, final IndyObjectMapper mapper,
                           SiteConfig location, String apiVersion, Map<String, String> mdcCopyMappings )
            throws IndyClientException
    {
        this.objectMapper = mapper;
        this.location = location;
        baseUrl = location.getUri();
        this.mdcCopyMappings = mdcCopyMappings;
        checkBaseUrl( baseUrl );
        addApiVersionHeader( apiVersion );
        initUserAgent( apiVersion );

        factory = new HttpFactory( authenticator );
    }

    public IndyClientHttp( final PasswordManager passwordManager, final IndyObjectMapper mapper,
                           SiteConfig location, String apiVersion )
            throws IndyClientException
    {
        this.objectMapper = mapper;
        this.location = location;
        baseUrl = location.getUri();
        checkBaseUrl( baseUrl );
        addApiVersionHeader( apiVersion );
        initUserAgent( apiVersion );

        factory = new HttpFactory( passwordManager );
    }

    private void initUserAgent( final String apiVersion )
    {
        String hcUserAgent =
                VersionInfo.getUserAgent( "Apache-HttpClient", "org.apache.http.client", HttpClientBuilder.class );

        String indyVersion = new IndyVersioningProvider().getVersioningInstance().getVersion();

        addDefaultHeader( "User-Agent", String.format("Indy/%s (api: %s) via %s", indyVersion, apiVersion, hcUserAgent ) );
    }

    private void addApiVersionHeader( String apiVersion )
    {
        if ( isNotBlank( apiVersion ) )
        {
            addDefaultHeader( HEADER_INDY_API_VERSION, apiVersion );
        }
    }

    private void checkBaseUrl( String baseUrl ) throws IndyClientException
    {
        try
        {
            new URL( baseUrl );
        }
        catch ( final MalformedURLException e )
        {
            throw new IndyClientException( "Invalid base-url: {}", e, baseUrl );
        }
    }

    /**
     * Not used since migration to jHTTPc library
     */
    @Deprecated
    public void connect( final HttpClientConnectionManager connectionManager )
            throws IndyClientException
    {
        // NOP, now that we've moved to HttpFactory.
    }

    /**
     * Not used since migration to jHTTPc library
     */
    @Deprecated
    public synchronized void connect()
    {
        // NOP, now that we've moved to HttpFactory.
    }

    public Map<String, String> head( final String path )
            throws IndyClientException
    {
        return head( path, HttpStatus.SC_OK );
    }

    public Map<String, String> head( final String path, final int... responseCodes )
            throws IndyClientException
    {
        connect();

        HttpHead request = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;

        try
        {
            request = newJsonHead( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(request);
            client = newClient();
            response = client.execute( request, newContext() );

            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                if ( sl.getStatusCode() == HttpStatus.SC_NOT_FOUND )
                {
                    return null;
                }

                throw new IndyClientException( sl.getStatusCode(), "Error executing HEAD: %s. Status was: %d %s (%s)",
                                               path, sl.getStatusCode(), sl.getReasonPhrase(),
                                               sl.getProtocolVersion() );
            }

            final Map<String, String> headers = new HashMap<>();
            for ( final Header header : response.getAllHeaders() )
            {
                final String name = header.getName().toLowerCase();

                if ( !headers.containsKey( name ) )
                {
                    headers.put( name, header.getValue() );
                }
            }

            return headers;
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( request, response, client );
        }
    }

    public <T> T get( final String path, final Class<T> type )
            throws IndyClientException
    {
        connect();

        HttpGet request = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            request = newJsonGet( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(request);
            response = client.execute( request, newContext() );

            final StatusLine sl = response.getStatusLine();

            if ( sl.getStatusCode() != 200 )
            {
                if ( sl.getStatusCode() == 404 )
                {
                    return null;
                }

                throw new IndyClientException( sl.getStatusCode(), "Error retrieving %s from: %s.\n%s",
                                               type.getSimpleName(), path, new IndyResponseErrorDetails( response ) );
            }

            final String json = entityToString( response );
            logger.debug( "Got JSON:\n\n{}\n\n", json );
            final T value = objectMapper.readValue( json, type );

            logger.debug( "Got result object: {}", value );

            return value;
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( request, response, client );
        }
    }

    public <T> T get( final String path, final TypeReference<T> typeRef )
            throws IndyClientException
    {
        connect();

        HttpGet request = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            request = newJsonGet( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(request);
            response = client.execute( request, newContext() );
            final StatusLine sl = response.getStatusLine();
            if ( sl.getStatusCode() != 200 )
            {
                if ( sl.getStatusCode() == 404 )
                {
                    return null;
                }

                throw new IndyClientException( sl.getStatusCode(), "Error retrieving %s from: %s.\n%s",
                                               typeRef.getType(), path, new IndyResponseErrorDetails( response ) );
            }

            final String json = entityToString( response );
            final T value = objectMapper.readValue( json, typeRef );

            return value;
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( request, response, client );
        }
    }

    public HttpResources getRaw( final HttpGet req )
            throws IndyClientException
    {
        connect();

        addLoggingMDCToHeaders(req);
        CloseableHttpResponse response = null;
        try
        {
            final CloseableHttpClient client = newClient();

            response = client.execute( req, newContext() );
            return new HttpResources( req, response, client );
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            // DO NOT CLOSE!!!! We're handing off control of the response to the caller!
            //            closeQuietly( response );
        }
    }

    public HttpResources getRaw( final String path )
            throws IndyClientException
    {
        return getRaw( path, Collections.singletonMap( "Accept", "*" ) );
    }

    public HttpResources getRaw( final String path, final Map<String, String> headers )
            throws IndyClientException
    {
        connect();

        CloseableHttpResponse response;
        try
        {
            final HttpGet req = newRawGet( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(req);
            if ( headers != null )
            {
                headers.forEach( (k, v) -> { req.setHeader( k, v );} );
            }
            final CloseableHttpClient client = newClient();

            response = client.execute( req, newContext() );
            return new HttpResources( req, response, client );
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            // DO NOT CLOSE!!!! We're handing off control of the response to the caller!
            //            closeQuietly( response );
        }
    }

    public void putWithStream( final String path, final InputStream stream )
            throws IndyClientException
    {
        putWithStream( path, stream, HttpStatus.SC_CREATED );
    }

    public void putWithStream( final String path, final InputStream stream, final int... responseCodes )
            throws IndyClientException
    {
        connect();

        final HttpPut put = newRawPut( buildUrl( baseUrl, path ) );
        addLoggingMDCToHeaders(put);
        final CloseableHttpClient client = newClient();
        CloseableHttpResponse response = null;
        try
        {
            put.setEntity( new InputStreamEntity( stream ) );

            response = client.execute( put, newContext() );
            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new ClientProtocolException(
                        new IndyClientException( sl.getStatusCode(), "Error in response from: %s.\n%s", path,
                                                 new IndyResponseErrorDetails( response ) ) );
            }

        }
        catch ( final ClientProtocolException e )
        {
            final Throwable cause = e.getCause();
            if ( cause != null && ( cause instanceof IndyClientException ) )
            {
                throw (IndyClientException) cause;
            }

            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( put, response, client );
        }
    }

    public boolean put( final String path, final Object value )
            throws IndyClientException
    {
        return put( path, value, HttpStatus.SC_OK, HttpStatus.SC_CREATED );
    }

    public boolean put( final String path, final Object value, final int... responseCodes )
            throws IndyClientException
    {
        checkRequestValue( value );

        connect();

        HttpPut put = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            put = newJsonPut( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(put);

            put.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            response = client.execute( put, newContext() );
            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new IndyClientException( sl.getStatusCode(), "Error in response from: %s.\n%s", path,
                                               new IndyResponseErrorDetails( response ) );
            }
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( put, response, client );
        }

        return true;
    }

    public HttpResources execute( HttpRequestBase request )
            throws IndyClientException
    {
        connect();

        addLoggingMDCToHeaders(request);
        CloseableHttpResponse response = null;
        try
        {
            final CloseableHttpClient client = newClient();

            response = client.execute( request, newContext() );
            return new HttpResources( request, response, client );
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            // DO NOT CLOSE!!!! We're handing off control of the response to the caller!
            //            closeQuietly( response );
        }
    }

    public HttpResources postRaw( final String path, Object value )
            throws IndyClientException
    {
        return postRaw( path, value, Collections.singletonMap( "Accept", "*" ) );
    }

    public HttpResources postRaw( final String path, Object value, final Map<String, String> headers )
            throws IndyClientException
    {
        checkRequestValue( value );
        connect();

        CloseableHttpResponse response = null;
        try
        {
            final HttpPost req = newRawPost( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(req);
            if ( headers != null )
            {
                for ( String key : headers.keySet() )
                {
                    req.setHeader( key, headers.get( key ) );
                }
            }

            req.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            final CloseableHttpClient client = newClient();

            response = client.execute( req, newContext() );
            return new HttpResources( req, response, client );
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            // DO NOT CLOSE!!!! We're handing off control of the response to the caller!
            //            closeQuietly( response );
        }
    }

    private void checkRequestValue( Object value )
            throws IndyClientException
    {
        if ( value == null )
        {
            throw new IndyClientException( "Cannot use null request value!" );
        }
    }

    public <T> T postWithResponse( final String path, final Object value, final Class<T> type )
            throws IndyClientException
    {
        return postWithResponse( path, value, type, HttpStatus.SC_CREATED, HttpStatus.SC_OK );
    }

    public <T> T postWithResponse( final String path, final Object value, final Class<T> type,
                                   final int... responseCodes )
            throws IndyClientException
    {
        checkRequestValue( value );

        connect();

        HttpPost post = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            post = newJsonPost( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(post);

            post.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            response = client.execute( post, newContext() );

            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new IndyClientException( sl.getStatusCode(), "Error POSTING with %s result from: %s.\n%s",
                                               type.getSimpleName(), path, new IndyResponseErrorDetails( response ) );
            }

            final String json = entityToString( response );
            return objectMapper.readValue( json, type );
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
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
            throws IndyClientException
    {
        return postWithResponse( path, value, typeRef, HttpStatus.SC_CREATED );
    }

    public <T> T postWithResponse( final String path, final Object value, final TypeReference<T> typeRef,
                                   final int... responseCodes )
            throws IndyClientException
    {
        checkRequestValue( value );

        connect();

        HttpPost post = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            post = newJsonPost( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(post);

            post.setEntity( new StringEntity( objectMapper.writeValueAsString( value ) ) );

            response = client.execute( post, newContext() );

            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new IndyClientException( sl.getStatusCode(), "Error retrieving %s from: %s.\n%s",
                                               typeRef.getType(), path, new IndyResponseErrorDetails( response ) );
            }

            final String json = entityToString( response );
            return objectMapper.readValue( json, typeRef );
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( post, response, client );
        }
    }

    @Override
    public void close()
    {
        logger.debug( "Shutting down indy client HTTP manager" );
        factory.shutdownNow();
    }

    /**
     * clean just the cached file (storage of groups and remote repos)
     */
    public void deleteCache( final String path )
                    throws IndyClientException
    {
        delete( path + "?" + CHECK_CACHE_ONLY + "=true" );
    }

    public void delete( final String path )
            throws IndyClientException
    {
        delete( path, HttpStatus.SC_NO_CONTENT, HttpStatus.SC_OK );
    }

    public void delete( final String path, final int... responseCodes )
            throws IndyClientException
    {
        connect();

        HttpDelete delete = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            delete = newDelete( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(delete);

            response = client.execute( delete, newContext() );
            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new IndyClientException( sl.getStatusCode(), "Error deleting: %s.\n%s", path,
                                               new IndyResponseErrorDetails( response ) );
            }
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( delete, response, client );
        }
    }

    public void deleteWithChangelog( final String path, final String changelog )
            throws IndyClientException
    {
        deleteWithChangelog( path, changelog, HttpStatus.SC_NO_CONTENT );
    }

    public void deleteWithChangelog( final String path, final String changelog, final int... responseCodes )
            throws IndyClientException
    {
        connect();

        HttpDelete delete = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            delete = newDelete( buildUrl( baseUrl, path ) );
            addLoggingMDCToHeaders(delete);
            delete.setHeader( ArtifactStore.METADATA_CHANGELOG, changelog );

            response = client.execute( delete, newContext() );
            final StatusLine sl = response.getStatusLine();
            if ( !validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                throw new IndyClientException( sl.getStatusCode(), "Error deleting: %s.\n%s", path,
                                               new IndyResponseErrorDetails( response ) );
            }
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
        finally
        {
            cleanupResources( delete, response, client );
        }
    }

    public boolean exists( final String path )
            throws IndyClientException
    {
        return exists( path, null, HttpStatus.SC_OK );
    }

    public boolean exists( final String path, Supplier<Map<String, String>> querySupplier )
            throws IndyClientException
    {
        return exists( path, querySupplier, HttpStatus.SC_OK );
    }

    public boolean exists( final String path, final int... responseCodes )
            throws IndyClientException
    {
        return exists( path, null, responseCodes );
    }

    public boolean exists( final String path, Supplier<Map<String, String>> querySupplier, final int... responseCodes )
            throws IndyClientException
    {
        connect();

        HttpHead request = null;
        CloseableHttpResponse response = null;
        CloseableHttpClient client = null;
        try
        {
            client = newClient();
            request = newJsonHead( buildUrl( baseUrl, querySupplier, path ) );
            addLoggingMDCToHeaders(request);

            response = client.execute( request, newContext() );
            final StatusLine sl = response.getStatusLine();
            if ( validResponseCode( sl.getStatusCode(), responseCodes ) )
            {
                return true;
            }
            else if ( sl.getStatusCode() == HttpStatus.SC_NOT_FOUND )
            {
                return false;
            }

            throw new IndyClientException( sl.getStatusCode(), "Error checking existence of: %s.\n%s", path,
                                           new IndyResponseErrorDetails( response ) );
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
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

    public String toIndyUrl( final String... path )
    {
        return buildUrl( baseUrl, path );
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public CloseableHttpClient newClient()
            throws IndyClientException
    {
        try
        {
            return factory.createClient( location, defaultHeaders );
        }
        catch ( JHttpCException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
    }

    public HttpClientContext newContext()
            throws IndyClientException
    {
        try
        {
            return factory.createContext( location );
        }
        catch ( JHttpCException e )
        {
            throw new IndyClientException( "Indy request failed: %s", e, e.getMessage() );
        }
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

    public HttpPost newRawPost( final String url )
    {
        final HttpPost req = new HttpPost( url );
        req.addHeader( "Content-Type", "application/json" );
        return req;
    }

    protected void addJsonHeaders( final HttpUriRequest req )
    {
        req.addHeader( "Accept", "application/json" );
        req.addHeader( "Content-Type", "application/json" );
    }

    public IndyObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    public static SiteConfig defaultSiteConfig( String baseUrl )
    {
        return new SiteConfigBuilder( "indy", baseUrl ).withRequestTimeoutSeconds( 30 )
                                                       .withMaxConnections( IndyClientHttp.GLOBAL_MAX_CONNECTIONS )
                                                       .build();
    }

    public void addDefaultHeader( String key, String value )
    {
        if ( defaultHeaders == null )
        {
            defaultHeaders = new ArrayList<>();
        }
        defaultHeaders.add( new BasicHeader( key, value ) );
    }

    private void addLoggingMDCToHeaders(HttpRequestBase request)
    {
        Map<String, String> context = MDC.getCopyOfContextMap();
        if (context == null) {
            return;
        }
        for (Map.Entry<String, String> mdcKeyHeaderKey : mdcCopyMappings.entrySet())
        {
            String mdcValue = context.get(mdcKeyHeaderKey.getKey());
            if (!StringUtils.isEmpty(mdcValue))
            {
                request.addHeader(mdcKeyHeaderKey.getValue(), mdcValue);
            }
        }
    }
}
