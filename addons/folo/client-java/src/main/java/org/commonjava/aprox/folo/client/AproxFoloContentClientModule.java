package org.commonjava.aprox.folo.client;

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

public class AproxFoloContentClientModule
    extends AproxClientModule
{

    public boolean exists( final String trackingId, final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        return http.exists( UrlUtils.buildUrl( "/folo/track", trackingId, type.singularEndpointName(), name, path ) );
    }

    public PathInfo store( final String trackingId, final StoreType type, final String name, final String path,
                           final InputStream stream )
        throws AproxClientException
    {
        http.putWithStream( UrlUtils.buildUrl( "/folo/track", trackingId, type.singularEndpointName(), name, path ),
                            stream );
        return getInfo( trackingId, type, name, path );
    }

    public PathInfo getInfo( final String trackingId, final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final Map<String, String> headers =
            http.head( UrlUtils.buildUrl( "/folo/track", trackingId, type.singularEndpointName(), name, path ) );
        return new PathInfo( headers );
    }

    public InputStream get( final String trackingId, final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final CloseableHttpResponse response =
            http.getRaw( UrlUtils.buildUrl( "/folo/track", trackingId, type.singularEndpointName(), name, path ) );

        if ( response.getStatusLine()
                     .getStatusCode() != 200 )
        {
            IOUtils.closeQuietly( response );
            throw new AproxClientException( "Response returned status: %d.", response.getStatusLine() );
        }

        return new ResponseManagingInputStream( response );
    }

}
