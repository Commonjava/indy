package org.commonjava.aprox.client.core.module;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.helper.HttpResources;
import org.commonjava.aprox.client.core.helper.PathInfo;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.DirectoryListingDTO;

public class AproxContentClientModule
    extends AproxClientModule
{

    public String contentUrl( final StoreType type, final String name, final String path )
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), type.singularEndpointName(), name, path );
    }

    public String contentUrl( final StoreKey key, final String path )
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), key.getType()
                                                        .singularEndpointName(), key.getName(), path );
    }

    public String contentPath( final StoreType type, final String name, final String path )
    {
        return UrlUtils.buildUrl( type.singularEndpointName(), name, path );
    }

    public String contentPath( final StoreKey key, final String path )
    {
        return UrlUtils.buildUrl( key.getType()
                                     .singularEndpointName(), key.getName(), path );
    }

    public DirectoryListingDTO listContents( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        String p = path;
        if ( !path.endsWith( "/" ) )
        {
            p += "/";
        }

        return http.get( contentPath( type, name, p ), DirectoryListingDTO.class );
    }

    public void delete( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        http.delete( contentPath( type, name, path ) );
    }

    public boolean exists( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        return http.exists( contentPath( type, name, path ) );
    }

    public PathInfo store( final StoreType type, final String name, final String path, final InputStream stream )
        throws AproxClientException
    {
        http.putWithStream( contentPath( type, name, path ), stream );
        return getInfo( type, name, path );
    }

    public PathInfo getInfo( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final Map<String, String> headers = http.head( contentPath( type, name, path ) );
        return new PathInfo( headers );
    }

    public InputStream get( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final HttpResources resources = http.getRaw( contentPath( type, name, path ) );

        if ( resources.getStatusCode() != 200 )
        {
            IOUtils.closeQuietly( resources );
            throw new AproxClientException( "Response returned status: %s.", resources.getStatusLine() );
        }

        return resources.getResponseStream();
    }

}
