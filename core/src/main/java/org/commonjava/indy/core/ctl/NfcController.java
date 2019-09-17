/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.commonjava.indy.model.core.StoreType.group;
import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.util.LocationUtils.toLocation;
import static org.commonjava.indy.util.LocationUtils.toLocations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.core.inject.AbstractNotFoundCache;
import org.commonjava.indy.core.model.Page;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.dto.NotFoundCacheDTO;
import org.commonjava.indy.model.core.dto.NotFoundCacheInfoDTO;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.core.model.DefaultPagination;
import org.commonjava.indy.core.model.Pagination;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class NfcController
{

    @Inject
    protected AbstractNotFoundCache cache;

    @Inject
    protected StoreDataManager storeManager;

    protected NfcController()
    {
    }

    public NfcController( final AbstractNotFoundCache nfc, final StoreDataManager stores )
    {
        this.cache = nfc;
        this.storeManager = stores;
    }
    
    public NotFoundCacheDTO getAllMissing()
    {
        Map<Location, Set<String>> allMissing = cache.getAllMissing();
        return getNotFoundCacheDTO( allMissing );
    }

    public Pagination<NotFoundCacheDTO> getAllMissing( Page page )
    {
        return new DefaultPagination<>( page, (handler)->
        {
            Map<Location, Set<String>> allMissing = cache.getAllMissing( page.getPageIndex(), page.getPageSize() );
            NotFoundCacheDTO dto = getNotFoundCacheDTO( allMissing );
            return dto;
        });
    }

    private NotFoundCacheDTO getNotFoundCacheDTO( Map<Location, Set<String>> allMissing )
    {
        final NotFoundCacheDTO dto = new NotFoundCacheDTO();
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
        return doGetMissing( key );
    }

    public Pagination<NotFoundCacheDTO> getMissing( final StoreKey key, Page page )
                    throws IndyWorkflowException
    {
        return new DefaultPagination<>( page, (handler)->
        {
            try
            {
                return doGetMissing( key, page.getPageIndex(), page.getPageSize() );
            }
            catch ( IndyWorkflowException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( e.getMessage(), e );
            }
            return null;
        });
    }

    //Warn: The getMissing is very expensive if group holds thousands of repositories.
    private final static int MAX_GROUP_MEMBER_SIZE_FOR_GET_MISSING = 300;

    private NotFoundCacheDTO doGetMissing( final StoreKey key, int... pagingParams )
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
                throw new IndyWorkflowException( "Failed to retrieve concrete constituent for: %s.", e, key );
            }

            if ( stores.size() >= MAX_GROUP_MEMBER_SIZE_FOR_GET_MISSING )
            {
                throw new IndyWorkflowException( SC_UNPROCESSABLE_ENTITY,
                                                 "Get missing for group failed (too many members), size: " + stores.size() );
            }

            final List<? extends KeyedLocation> locations = toLocations( stores );
            for ( final KeyedLocation location : locations )
            {
                Set<String> missing;
                if ( pagingParams != null && pagingParams.length > 0 )
                {
                    missing = cache.getMissing( location, pagingParams[0], pagingParams[1] );
                }
                else
                {
                    missing = cache.getMissing( location );
                }

                if ( missing != null && !missing.isEmpty() )
                {
                    final List<String> paths = new ArrayList<>( missing );
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
                Set<String> missing;
                if ( pagingParams != null && pagingParams.length > 0 )
                {
                    missing = cache.getMissing( toLocation( store ), pagingParams[0], pagingParams[1] );
                }
                else
                {
                    missing = cache.getMissing( toLocation( store ) );
                }

                final List<String> paths = new ArrayList<>( missing );
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
        if ( store.getKey().getType() == remote || store.getKey().getType() == hosted )
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

    public NotFoundCacheInfoDTO getInfo()
    {
        NotFoundCacheInfoDTO dto = new NotFoundCacheInfoDTO();
        dto.setSize( cache.getSize() );
        return dto;
    }

    public NotFoundCacheInfoDTO getInfo( StoreKey key ) throws IndyWorkflowException
    {
        NotFoundCacheInfoDTO dto = new NotFoundCacheInfoDTO();
        final AtomicLong size = new AtomicLong( 0);
        try
        {
            switch ( key.getType() )
            {
                case group:
                {
                    //Warn: This is very expensive if group holds thousands of repositories
                    final List<StoreKey> stores = storeManager.query()
                                                              .packageType( key.getPackageType() )
                                                              .getOrderedConcreteStoresInGroup( key.getName() )
                                                              .stream()
                                                              .map( artifactStore -> artifactStore.getKey() )
                                                              .collect( Collectors.toList() );

                    if ( stores.size() >= MAX_GROUP_MEMBER_SIZE_FOR_GET_MISSING )
                    {
                        throw new IndyWorkflowException( SC_UNPROCESSABLE_ENTITY,
                                                         "Get missing info for group failed (too many members), size: "
                                                                         + stores.size() );
                    }

                    for ( final StoreKey storeKey : stores )
                    {
                        size.addAndGet( cache.getSize( storeKey ) );
                    }
                    break;
                }
                default:
                {
                    size.addAndGet( cache.getSize( key ) );
                    break;
                }
            }
            dto.setSize( size.get() );
            return dto;
        }
        catch ( final IndyDataException e )
        {
            throw new IndyWorkflowException( "Failed to get info for ArtifactStore: %s.", e, key );
        }
    }
}
