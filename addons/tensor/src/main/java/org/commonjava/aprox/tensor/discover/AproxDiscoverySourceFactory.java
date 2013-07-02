package org.commonjava.aprox.tensor.discover;

import static org.commonjava.aprox.tensor.util.AproxTensorUtils.toDiscoveryURI;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.tensor.discover.DiscoverySourceFactory;

@ApplicationScoped
public class AproxDiscoverySourceFactory
    implements DiscoverySourceFactory
{

    @Override
    public URI createSourceURI( final String source )
    {
        final StoreKey key = StoreKey.fromString( source );
        return toDiscoveryURI( key );
    }

    @Override
    public String getFormatHint()
    {
        return "<store-type>:<store-name>";
    }

}
