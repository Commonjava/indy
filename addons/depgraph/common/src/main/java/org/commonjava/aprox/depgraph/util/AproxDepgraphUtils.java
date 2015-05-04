/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
