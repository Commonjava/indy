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
package org.commonjava.aprox.depgraph.discover;

import static org.commonjava.aprox.depgraph.util.AproxDepgraphUtils.APROX_URI_PREFIX;
import static org.commonjava.aprox.depgraph.util.AproxDepgraphUtils.toDiscoveryURI;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Production
@Default
public class AproxDiscoverySourceManager
    implements DiscoverySourceManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

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
        logger.debug( "Got source-URI store-key: '{}'", key );
        final URI result = toDiscoveryURI( key );
        logger.debug( "Resulting source URI: '{}'", result );
        return result;
    }

    private String normalize( final String source )
    {
        logger.debug( "Normalizing source URI: '{}'", source );
        if ( source.startsWith( APROX_URI_PREFIX ) )
        {
            final String result = source.substring( APROX_URI_PREFIX.length() );
            logger.debug( "Normalized source URI: '{}'", result );
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
    public boolean activateWorkspaceSources( final ViewParams params, final Collection<? extends Location> locations )
        throws CartoDataException
    {
        if ( locations == null || locations.isEmpty() )
        {
            return false;
        }

        final Set<String> sources = new HashSet<String>( locations.size() );
        for ( final Location loc : locations )
        {
            sources.add( loc.getUri() );
        }

        return activateWorkspaceSources( params, sources.toArray( new String[sources.size()] ) );
    }

    @Override
    public boolean activateWorkspaceSources( final ViewParams params, final String... sources )
        throws CartoDataException
    {
        boolean result = false;
        if ( params != null )
        {
            for ( final String src : sources )
            {
                final StoreKey key = StoreKey.fromString( normalize( src ) );
                if ( key.getType() == StoreType.group )
                {
                    try
                    {
                        final List<ArtifactStore> orderedStores =
                            stores.getOrderedConcreteStoresInGroup( key.getName() );
                        for ( final ArtifactStore store : orderedStores )
                        {
                            final URI uri = toDiscoveryURI( store.getKey() );
                            if ( params.getActiveSources()
                                       .contains( uri ) )
                            {
                                continue;
                            }

                            params.addActiveSource( uri );
                            result = result || params.getActiveSources()
                                                     .contains( uri );
                        }
                    }
                    catch ( final ProxyDataException e )
                    {
                        throw new CartoDataException( "Failed to lookup ordered concrete stores for: {}. Reason: {}",
                                                      e, key, e.getMessage() );
                    }
                }
                else
                {
                    final URI uri = toDiscoveryURI( key );
                    if ( !params.getActiveSources()
                                .contains( uri ) )
                    {
                        params.addActiveSource( uri );
                        result = result || params.getActiveSources()
                                                 .contains( uri );
                    }
                }
            }
        }

        return result;
    }

    @Override
    public Location createLocation( final Object source )
    {
        final StoreKey key = StoreKey.fromString( normalize( source.toString() ) );
        ArtifactStore store = null;
        try
        {
            store = stores.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            logger.error( String.format( "Failed to lookup ArtifactStore for key: {}. Reason: {}", key, e.getMessage()  ), e );
        }
        
        return store == null ? null : LocationUtils.toLocation( store );
    }

    @Override
    public List<? extends Location> createLocations( final Object... sources )
    {
        return createLocations( Arrays.asList( sources ) );
    }

    @Override
    public List<? extends Location> createLocations( final Collection<Object> sources )
    {
        final List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        for ( final Object source : sources )
        {
            final StoreKey key = StoreKey.fromString( normalize( source.toString() ) );
            
            ArtifactStore store = null;
            try
            {
                store = this.stores.getArtifactStore( key );
            }
            catch ( final ProxyDataException e )
            {
                    logger.error( String.format( "Failed to lookup ArtifactStore for key: {}. Reason: {}", key, e.getMessage()  ), e );
            }
            
            if ( store != null )
            {
                stores.add( store );
            }
        }

        return LocationUtils.toLocations( stores );
    }

}
