package org.commonjava.aprox.depgraph.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.commonjava.aprox.model.StoreKey;

public final class AproxDepgraphUtils
{

    public static final String APROX_SCHEME = "aprox";

    private AproxDepgraphUtils()
    {
    }

    public static StoreKey getDiscoveryStore( final URI source )
    {
        final String scheme = source.getScheme();
        if ( !APROX_SCHEME.equals( scheme ) )
        {
            throw new UnsupportedOperationException(
                                                     "Discovery from arbitrary external locations is not supported within AProx. Instead, use 'aprox:<groupname>' as the discovery source." );
        }

        // of the form: aprox:group:my-group where 'aprox is the scheme and 'group:my-group' is the scheme specific-part
        final String keyname = source.getSchemeSpecificPart();
        return StoreKey.fromString( keyname );
    }

    public static URI toDiscoveryURI( final StoreKey key )
    {
        try
        {
            return new URI( APROX_SCHEME + ":" + key.getType()
                                                    .name() + ":" + key.getName() );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Failed to construct URI for ArtifactStore: " + key + ". Reason: "
                + e.getMessage(), e );
        }
    }

}
