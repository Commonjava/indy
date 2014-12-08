/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.depgraph.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.commonjava.aprox.model.core.StoreKey;

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
