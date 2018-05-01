/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.ctl;

import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.util.LocationUtils.toLocation;
import static org.commonjava.indy.util.LocationUtils.toLocations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.NotFoundCacheDTO;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;

@ApplicationScoped
public class NfcController
{

    @Inject
    protected NotFoundCache cache;

    @Inject
    protected StoreDataManager storeManager;

    protected NfcController()
    {
    }

    public NfcController( final NotFoundCache nfc, final StoreDataManager stores )
    {
        this.cache = nfc;
        this.storeManager = stores;
    }
    
    public NotFoundCacheDTO getAllMissing()
    {
        final NotFoundCacheDTO dto = new NotFoundCacheDTO();
        final Map<Location, Set<String>> allMissing = cache.getAllMissing();
        for ( final Location loc : allMissing.keySet() )
        {
            if ( loc instanceof KeyedLocation )
            {
                final List<String> paths = new ArrayList<String>( allMissing.get( loc ) );
                Collections.sort( paths );

                dto.addSection( ( (KeyedLocation) loc ).getKey(), paths );
            }
        }

        return dto;
    }

    public NotFoundCacheDTO getMissing( final StoreKey key )
        throws IndyWorkflowException
    {
        final NotFoundCacheDTO dto = new NotFoundCacheDTO();
        if ( key.getType() == group )
        {
            List<ArtifactStore> stores;
            try
            {
                stores = storeManager.query().packageType( key.getPackageType() ).getOrderedConcreteStoresInGroup( key.getName() );
            }
            catch ( final IndyDataException e )
            {
                throw new IndyWorkflowException( "Failed to retrieve concrete constituent ArtifactStores for: %s.", e,
                                                  key );
            }

            final List<? extends KeyedLocation> locations = toLocations( stores );
            for ( final KeyedLocation location : locations )
            {
                final Set<String> missing = cache.getMissing( location );
                if ( missing != null && !missing.isEmpty() )
                {
                    final List<String> paths = new ArrayList<String>( missing );
                    Collections.sort( paths );

                    dto.addSection( location.getKey(), paths );
                }
            }
        }
        else
        {
            ArtifactStore store;
            try
            {
                store = storeManager.getArtifactStore( key );
            }
            catch ( final IndyDataException e )
            {
                throw new IndyWorkflowException( "Failed to retrieve ArtifactStore: %s.", e, key );
            }

            if ( store != null )
            {
                final Set<String> missing = cache.getMissing( toLocation( store ) );
                final List<String> paths = new ArrayList<String>( missing );
                Collections.sort( paths );

                dto.addSection( key, paths );
            }
        }

        return dto;
    }

    public void clear()
    {
        cache.clearAllMissing();
    }

    public void clear( final StoreKey key )
        throws IndyWorkflowException
    {
        clear( key, null );
    }

    public void clear( final StoreKey key, final String path )
        throws IndyWorkflowException
    {
        try
        {
            switch ( key.getType() )
            {
                case group:
                {
                    final List<ArtifactStore> stores = storeManager.query().packageType( key.getPackageType() ).getOrderedConcreteStoresInGroup( key.getName() );
                    for ( final ArtifactStore store : stores )
                    {
                        clear( store, path );
                    }
                    break;
                }
                default:
                {
                    storeManager.query()
                                .packageType( key.getPackageType() )
                                .concreteStores()
                                .stream( s -> s.getName().equals( key.getName() ) )
                                .forEach( store -> clear( store, path ) );
                    break;
                }
            }
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( "Failed to retrieve ArtifactStore: %s.", e, key );
        }
    }

    private void clear( final ArtifactStore store, final String path )
    {
        if ( store.getKey()
                  .getType() == remote )
        {
            final Location loc = toLocation( store );
            if ( loc != null )
            {
                if ( path != null )
                {
                    cache.clearMissing( new ConcreteResource( loc, path ) );
                }
                else
                {
                    cache.clearMissing( loc );
                }
            }
        }
    }

}
