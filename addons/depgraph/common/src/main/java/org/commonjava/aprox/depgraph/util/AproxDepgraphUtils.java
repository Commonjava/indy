/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.commonjava.aprox.model.StoreKey;

public final class AproxDepgraphUtils
{

    public static final String APROX_SCHEME = "aprox";

    public static final String APROX_URI_PREFIX = APROX_SCHEME + ":";

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
            return new URI( APROX_URI_PREFIX + key.getType()
                                                  .name() + ":" + key.getName() );
        }
        catch ( final URISyntaxException e )
        {
            throw new IllegalStateException( "Failed to construct URI for ArtifactStore: " + key + ". Reason: " + e.getMessage(), e );
        }
    }

}
