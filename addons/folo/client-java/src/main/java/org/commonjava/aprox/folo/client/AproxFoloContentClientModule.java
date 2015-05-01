package org.commonjava.aprox.folo.client;

import java.io.IOException;
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
            if ( resources.getStatusCode() == 404 )
            {
                return null;
            }

            IOUtils.closeQuietly( resources );
            throw new AproxClientException( "Response returned status: %s.", resources.getStatusLine() );
        }

        try
        {
            return resources.getResponseStream();
        }
        catch ( final IOException e )
        {
            throw new AproxClientException( "Failed to open response content stream: %s", e, e.getMessage() );
        }
    }

    public String contentUrl( final String trackingId, final StoreType type, final String name, final String... path )
    {
        return UrlUtils.buildUrl( http.getBaseUrl(), aggregatePathParts( trackingId, type, name, path ) );
    }

    public String contentPath( final String trackingId, final StoreType type, final String name, final String... path )
    {
        return UrlUtils.buildUrl( null, aggregatePathParts( trackingId, type, name, path ) );
    }

    private String[] aggregatePathParts( final String trackingId, final StoreType type, final String name,
                                         final String... path )
    {
        final String[] parts = new String[path.length + 3];
        parts[0] = trackingId;
        parts[1] = type.singularEndpointName();
        parts[2] = name;
        System.arraycopy( path, 0, parts, 2, path.length );

        return parts;
    }

}
