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
package org.commonjava.aprox.depgraph.discover;

import static org.commonjava.aprox.depgraph.util.AproxDepgraphUtils.APROX_URI_PREFIX;
import static org.commonjava.aprox.depgraph.util.AproxDepgraphUtils.toDiscoveryURI;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
@Production
@Default
public class AproxDiscoverySourceManager
    implements DiscoverySourceManager
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager stores;

    protected AproxDiscoverySourceManager()
    {
    }

    public AproxDiscoverySourceManager( final StoreDataManager stores )
    {
        this.stores = stores;
    }

    @Override
    public URI createSourceURI( final String source )
    {
        final StoreKey key = StoreKey.fromString( normalize( source ) );
        logger.info( "Got source-URI store-key: '%s'", key );
        final URI result = toDiscoveryURI( key );
        logger.info( "Resulting source URI: '%s'", result );
        return result;
    }

    private String normalize( final String source )
    {
        logger.info( "Normalizing source URI: '%s'", source );
        if ( source.startsWith( APROX_URI_PREFIX ) )
        {
            final String result = source.substring( APROX_URI_PREFIX.length() );
            logger.info( "Normalized source URI: '%s'", result );
            return result;
        }

        return source;
    }

    @Override
    public String getFormatHint()
    {
        return "<store-type>:<store-name>";
    }

    @Override
    public void activateWorkspaceSources( final GraphWorkspace ws, final String... sources )
        throws CartoDataException
    {
        if ( ws != null )
        {
            for ( final String src : sources )
            {
                final StoreKey key = StoreKey.fromString( normalize( src ) );
                if ( key.getType() == StoreType.group )
                {
                    try
                    {
                        final List<ArtifactStore> orderedStores = stores.getOrderedConcreteStoresInGroup( key.getName() );
                        for ( final ArtifactStore store : orderedStores )
                        {
                            ws.addActiveSource( toDiscoveryURI( store.getKey() ) );
                        }
                    }
                    catch ( final ProxyDataException e )
                    {
                        throw new CartoDataException( "Failed to lookup ordered concrete stores for: %s. Reason: %s", e, key, e.getMessage() );
                    }
                }
                else
                {
                    ws.addActiveSource( toDiscoveryURI( key ) );
                }
            }
        }
    }

    @Override
    public Location createLocation( final Object source )
    {
        final StoreKey key = StoreKey.fromString( normalize( source.toString() ) );
        return LocationUtils.toCacheLocation( key );
    }

    @Override
    public List<? extends Location> createLocations( final Object... sources )
    {
        final List<StoreKey> keys = new ArrayList<StoreKey>( sources.length );
        for ( final Object source : sources )
        {
            keys.add( StoreKey.fromString( normalize( source.toString() ) ) );
        }

        return LocationUtils.toCacheLocations( keys );
    }

    @Override
    public List<? extends Location> createLocations( final Collection<Object> sources )
    {
        final List<StoreKey> keys = new ArrayList<StoreKey>( sources.size() );
        for ( final Object source : sources )
        {
            keys.add( StoreKey.fromString( normalize( source.toString() ) ) );
        }

        return LocationUtils.toCacheLocations( keys );
    }

}
