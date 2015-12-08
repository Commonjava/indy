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
package org.commonjava.indy.client.core.module;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.client.core.util.UrlUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.DirectoryListingDTO;

public class IndyContentClientModule
    extends IndyClientModule
{

    public String contentUrl( final StoreType type, final String name, final String... path )
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), aggregatePathParts( type, name, path ) );
    }

    private String[] aggregatePathParts( final StoreKey key, final String... path )
    {
        return aggregatePathParts( key.getType(), key.getName(), path );
    }

    private String[] aggregatePathParts( final StoreType type, final String name, final String... path )
    {
        final String[] parts = new String[path.length + 2];
        parts[0] = type.singularEndpointName();
        parts[1] = name;
        System.arraycopy( path, 0, parts, 2, path.length );

        return parts;
    }

    public String contentUrl( final StoreKey key, final String... path )
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), aggregatePathParts( key, path ) );
    }

    public String contentPath( final StoreType type, final String name, final String... path )
    {
        return UrlUtils.buildUrl( null, aggregatePathParts( type, name, path ) );
    }

    public String contentPath( final StoreKey key, final String... path )
    {
        return UrlUtils.buildUrl( null, aggregatePathParts( key, path ) );
    }

    public DirectoryListingDTO listContents( final StoreKey key, final String path )
            throws IndyClientException
    {
        return listContents( key.getType(), key.getName(), path );
    }

    public DirectoryListingDTO listContents( final StoreType type, final String name, final String path )
        throws IndyClientException
    {
        String p = path;
        if ( !path.endsWith( "/" ) )
        {
            p += "/";
        }

        return http.get( contentPath( type, name, p ), DirectoryListingDTO.class );
    }

    public void delete( final StoreKey key, final String path )
            throws IndyClientException
    {
        http.delete( contentPath( key.getType(), key.getName(), path ) );
    }

    public void delete( final StoreType type, final String name, final String path )
        throws IndyClientException
    {
        http.delete( contentPath( type, name, path ) );
    }

    public boolean exists( final StoreKey key, final String path )
            throws IndyClientException
    {
        return http.exists( contentPath( key.getType(), key.getName(), path ) );
    }

    public boolean exists( final StoreType type, final String name, final String path )
        throws IndyClientException
    {
        return http.exists( contentPath( type, name, path ) );
    }

    public void store( final StoreKey key, final String path, final InputStream stream )
            throws IndyClientException
    {
        http.putWithStream( contentPath( key.getType(), key.getName(), path ), stream );
    }

    public void store( final StoreType type, final String name, final String path, final InputStream stream )
        throws IndyClientException
    {
        http.putWithStream( contentPath( type, name, path ), stream );
    }

    public PathInfo getInfo( final StoreKey key, final String path )
        throws IndyClientException
    {
        final Map<String, String> headers = http.head( contentPath( key.getType(), key.getName(), path ) );
        return new PathInfo( headers );
    }

    public PathInfo getInfo( final StoreType type, final String name, final String path )
            throws IndyClientException
    {
        final Map<String, String> headers = http.head( contentPath( type, name, path ) );
        return new PathInfo( headers );
    }

    public InputStream get( final StoreKey key, final String path )
            throws IndyClientException
    {
        return get( key.getType(), key.getName(), path );
    }

    public InputStream get( final StoreType type, final String name, final String path )
        throws IndyClientException
    {
        final HttpResources resources = http.getRaw( contentPath( type, name, path ) );

        if ( resources.getStatusCode() != 200 )
        {
            if ( resources.getStatusCode() == 404 )
            {
                return null;
            }

            IOUtils.closeQuietly( resources );
            throw new IndyClientException( resources.getStatusCode(), "Response returned status: %s.",
                                            resources.getStatusLine() );
        }

        try
        {
            return resources.getResponseStream();
        }
        catch ( final IOException e )
        {
            throw new IndyClientException( "Failed to open response content stream: %s", e,
                                            e.getMessage() );
        }
    }

}
