/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.depgraph;

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
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoveryResult;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Logger;

public class DepgraphStorageListenerRunnable
    implements Runnable
{

    private final Logger logger = new Logger( getClass() );

    private final Transfer item;

    private DiscoveryResult result;

    private final AproxModelDiscoverer discoverer;

    private CartoDataException error;

    private final StoreDataManager aprox;

    public DepgraphStorageListenerRunnable( final AproxModelDiscoverer discoverer, final StoreDataManager aprox, final Transfer item )
    {
        this.discoverer = discoverer;
        this.aprox = aprox;
        this.item = item;
    }

    public DiscoveryResult getResult()
    {
        return result;
    }

    @Override
    public void run()
    {
        final StoreKey key = LocationUtils.getKey( item );

        ArtifactStore originatingStore = null;
        try
        {
            originatingStore = aprox.getArtifactStore( key );
        }
        catch ( final ProxyDataException e )
        {
            error = new CartoDataException( "Failed to retrieve store for: %s. Reason: %s", e, key, e.getMessage() );
        }

        if ( originatingStore == null )
        {
            return;
        }

        //        logger.info( "Logging: %s with Tensor relationship-graphing system.", event );
        final List<ArtifactStore> stores = getRelevantStores( originatingStore );
        if ( stores == null || stores.isEmpty() )
        {
            error = new CartoDataException( "No stores found for: %s.", key );
        }

        if ( error != null )
        {
            return;
        }

        final List<? extends KeyedLocation> locations = LocationUtils.toLocations( stores );

        try
        {
            result = discoverer.discoverRelationships( item, locations, true );
        }
        catch ( final CartoDataException e )
        {
            error = e;
        }
    }

    public DiscoveryResult getDiscoveryResult()
    {
        return result;
    }

    public CartoDataException getError()
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
            logger.error( "Cannot lookup full store list for groups containing artifact store: %s. Reason: %s", e, originatingStore.getKey(),
                          e.getMessage() );
            stores = null;
        }

        return stores;
    }

}
