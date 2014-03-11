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
package org.commonjava.aprox.depgraph;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.discover.AproxModelDiscoverer;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.ArtifactPathInfo;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.discover.DefaultDiscoveryConfig;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.cartographer.discover.post.patch.PatcherSupport;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DepgraphStorageListenerRunnable
    implements Runnable
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Transfer item;

    private DiscoveryResult result;

    private final AproxModelDiscoverer discoverer;

    private Throwable error;

    private final StoreDataManager aprox;

    private final CartoDataManager carto;

    private final PatcherSupport patcherSupport;

    public DepgraphStorageListenerRunnable( final AproxModelDiscoverer discoverer, final StoreDataManager aprox, final CartoDataManager carto,
                                            final PatcherSupport patcherSupport, final Transfer item )
    {
        this.discoverer = discoverer;
        this.aprox = aprox;
        this.carto = carto;
        this.patcherSupport = patcherSupport;
        this.item = item;
    }

    public DiscoveryResult getResult()
    {
        return result;
    }

    @Override
    public void run()
    {
        final ArtifactPathInfo info = ArtifactPathInfo.parse( item.getPath() );
        if ( info == null )
        {
            logger.info( "Cannot parse path into GAV: {}", item.getPath() );
            return;
        }

        final ProjectVersionRef ref = info.getProjectId();
        final StoreKey key = LocationUtils.getKey( item );

        ArtifactStore originatingStore = null;
        try
        {
            originatingStore = aprox.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            error = new CartoDataException( "Failed to retrieve store for: {}. Reason: {}", e, key, e.getMessage() );
        }

        if ( originatingStore == null )
        {
            return;
        }

        //        logger.info( "Logging: {} in project-dependency graph.", event );
        final List<ArtifactStore> stores = getRelevantStores( originatingStore );
        if ( stores == null || stores.isEmpty() )
        {
            error = new CartoDataException( "No stores found for: {}.", key );
        }

        if ( error != null )
        {
            return;
        }

        final List<? extends KeyedLocation> locations = LocationUtils.toLocations( stores );

        try
        {
            final DefaultDiscoveryConfig config = new DefaultDiscoveryConfig( item.getLocation() );
            config.setLocations( locations );
            config.setStoreRelationships( true );
            config.setEnabledPatchers( patcherSupport.getAvailablePatchers() );

            carto.setCurrentWorkspace( key.toString() );

            result = discoverer.discoverRelationships( ref, item, config );

            carto.getCurrentWorkspace()
                 .getDatabase()
                 .printStats();
        }
        catch ( final CartoDataException e )
        {
            error = e;
        }
        catch ( final URISyntaxException e )
        {
            error = e;
        }
        finally
        {
            try
            {
                carto.clearCurrentWorkspace();
            }
            catch ( final CartoDataException e )
            {
                logger.error( String.format( "Failed to clear workspace for: %s. Reason: %s", key, e.getMessage() ), e );
            }
        }
    }

    public DiscoveryResult getDiscoveryResult()
    {
        return result;
    }

    public Throwable getError()
    {
        return error;
    }

    private List<ArtifactStore> getRelevantStores( final ArtifactStore originatingStore )
    {
        List<ArtifactStore> stores = new ArrayList<ArtifactStore>();
        stores.add( originatingStore );

        try
        {
            final Set<? extends Group> groups = aprox.getGroupsContaining( originatingStore.getKey() );
            for ( final Group group : groups )
            {
                if ( group == null )
                {
                    continue;
                }

                final List<? extends ArtifactStore> orderedStores = aprox.getOrderedConcreteStoresInGroup( group.getName() );

                if ( orderedStores != null )
                {
                    for ( final ArtifactStore as : orderedStores )
                    {
                        if ( as == null || stores.contains( as ) )
                        {
                            continue;
                        }

                        stores.add( as );
                    }
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Cannot lookup full store list for groups containing artifact store: {}. Reason: {}", e, originatingStore.getKey(),
                          e.getMessage() );
            stores = null;
        }

        return stores;
    }

}
