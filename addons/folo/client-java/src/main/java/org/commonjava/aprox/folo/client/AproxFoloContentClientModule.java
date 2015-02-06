package org.commonjava.aprox.folo.client;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.helper.HttpResources;
import org.commonjava.aprox.client.core.helper.PathInfo;
import org.commonjava.aprox.client.core.util.UrlUtils;
import org.commonjava.aprox.model.core.StoreType;

public class AproxFoloContentClientModule
    extends AproxClientModule
{

    private static final String TRACKING_PATH = "/folo/track";

    public String trackingUrl( final String id, final StoreType type, final String name )
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), TRACKING_PATH, id, type.singularEndpointName(), name );
    }

    public boolean exists( final String trackingId, final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        return http.exists( UrlUtils.buildUrl( TRACKING_PATH, trackingId, type.singularEndpointName(), name, path ) );
    }

    public PathInfo store( final String trackingId, final StoreType type, final String name, final String path,
                           final InputStream stream )
        throws AproxClientException
    {
        http.putWithStream( UrlUtils.buildUrl( TRACKING_PATH, trackingId, type.singularEndpointName(), name, path ),
                            stream );
        return getInfo( trackingId, type, name, path );
    }

    public PathInfo getInfo( final String trackingId, final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final Map<String, String> headers =
            http.head( UrlUtils.buildUrl( TRACKING_PATH, trackingId, type.singularEndpointName(), name, path ) );
        return new PathInfo( headers );
    }

    public InputStream get( final String trackingId, final StoreType type, final String name, final String path )
        throws AproxClientException
    {
        final HttpResources resources =
            http.getRaw( UrlUtils.buildUrl( TRACKING_PATH, trackingId, type.singularEndpointName(), name, path ) );

        if ( resources.getStatusCode() != 200 )
        {
            IOUtils.closeQuietly( resources );
            throw new AproxClientException( "Response returned status: %d.", resources.getStatusLine() );
        }

        return resources.getResponseStream();
    }

}
