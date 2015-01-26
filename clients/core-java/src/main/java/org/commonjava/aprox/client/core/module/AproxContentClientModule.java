package org.commonjava.aprox.client.core.module;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.helper.PathInfo;
import org.commonjava.aprox.client.core.util.ResponseManagingInputStream;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.DirectoryListingDTO;

public class AproxContentClientModule
    extends AproxClientModule
{

    public String contentUrl( final StoreType type, final String name, final String path )
    {
        return UrlUtils.buildUrl( type.singularEndpointName(), name, path );
    }

    public String contentUrl( final StoreKey key, final String path )
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

        return http.get( contentUrl( type, name, p ), DirectoryListingDTO.class );
    }

    public void delete( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        http.delete( contentUrl( type, name, path ) );
    }

    public boolean exists( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        return http.exists( contentUrl( type, name, path ) );
    }

    public PathInfo store( final StoreType type, final String name, final String path, final InputStream stream )
        throws AproxClientException
    {
        http.putWithStream( contentUrl( type, name, path ), stream );
        return getInfo( type, name, path );
    }

    public PathInfo getInfo( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final Map<String, String> headers = http.head( contentUrl( type, name, path ) );
        return new PathInfo( headers );
    }

    public InputStream get( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final CloseableHttpResponse response =
 http.getRaw( contentUrl( type, name, path ) );

        if ( response.getStatusLine()
                     .getStatusCode() != 200 )
        {
            IOUtils.closeQuietly( response );
            throw new AproxClientException( "Response returned status: %s.", response.getStatusLine() );
        }

        return new ResponseManagingInputStream( response );
    }

}
