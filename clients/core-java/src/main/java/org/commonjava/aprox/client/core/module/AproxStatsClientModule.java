package org.commonjava.aprox.client.core.module;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.client.core.helper.HttpResources;
import org.commonjava.aprox.model.core.dto.EndpointViewListing;
import org.commonjava.aprox.model.spi.AddOnListing;
import org.commonjava.aprox.stats.AProxVersioning;

public class AproxStatsClientModule
    extends AproxClientModule
{
    
    public AProxVersioning getVersionInfo()
        throws AproxClientException
    {
        return getHttp().get( "/stats/version-info", AProxVersioning.class );
    }
    
    public EndpointViewListing getAllEndpoints()
        throws AproxClientException
    {
        return getHttp().get( "/stats/all-endpoints", EndpointViewListing.class );
    }
    
    public AddOnListing getActiveAddons()
        throws AproxClientException
    {
        return getHttp().get( "/stats/addons/active", AddOnListing.class );
    }

    public InputStream getActiveAddonsJS()
        throws AproxClientException
    {
        final HttpResources resources = getHttp().getRaw( "/stats/addons/active.js" );

        if ( resources.getStatusCode() != 200 )
        {
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

}
