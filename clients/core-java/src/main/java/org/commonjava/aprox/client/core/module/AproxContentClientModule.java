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
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.DirectoryListingDTO;

public class AproxContentClientModule
    extends AproxClientModule
{

    public DirectoryListingDTO listContents( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        String p = path;
        if ( !path.endsWith( "/" ) )
        {
            p += "/";
        }

        return http.get( UrlUtils.buildUrl( type.singularEndpointName(), name, p ), DirectoryListingDTO.class );
    }

    public void delete( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        http.delete( UrlUtils.buildUrl( type.singularEndpointName(), name, path ) );
    }

    public boolean exists( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        return http.exists( UrlUtils.buildUrl( type.singularEndpointName(), name, path ) );
    }

    public PathInfo store( final StoreType type, final String name, final String path, final InputStream stream )
        throws AproxClientException
    {
        http.putWithStream( UrlUtils.buildUrl( type.singularEndpointName(), name, path ), stream );
        return getInfo( type, name, path );
    }

    public PathInfo getInfo( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final Map<String, String> headers = http.head( UrlUtils.buildUrl( type.singularEndpointName(), name, path ) );
        return new PathInfo( headers );
    }

    public InputStream get( final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final CloseableHttpResponse response =
            http.getRaw( UrlUtils.buildUrl( type.singularEndpointName(), name, path ) );

        if ( response.getStatusLine()
                     .getStatusCode() != 200 )
        {
            IOUtils.closeQuietly( response );
            throw new AproxClientException( "Response returned status: %d.", response.getStatusLine() );
        }

        return new ResponseManagingInputStream( response );
    }

}
