/**
 * Copyright (C) 2020 Red Hat, Inc.
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
package org.commonjava.indy.db.service;

import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.module.IndyStoreQueryClientModule;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.SimpleBooleanResultDTO;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.pkg.PackageTypeConstants;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.commonjava.indy.model.core.StoreType.group;

@SuppressWarnings( "unchecked" )
public class ServiceStoreQuery<T extends ArtifactStore>
        implements ArtifactStoreQuery<T>
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private final ServiceStoreDataManager dataManager;

    private final Indy client;

    private String packageType = MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

    final static String ARTIFACT_STORE_QUERY = "artifact-store-query";

    private final Integer STORE_QUERY_EXPIRATION_IN_MINS = 15;

    private final CacheProducer cacheProducer;

    private Set<StoreType> types;

    private Boolean enabled;

    public ServiceStoreQuery( final ServiceStoreDataManager dataManager, final CacheProducer producer )
    {
        logger.debug( "CREATE new default store query with data manager only" );
        this.dataManager = dataManager;
        this.cacheProducer = producer;
        this.client = dataManager.getIndyClient();
    }

    @Override
    public <C extends ArtifactStore> ArtifactStoreQuery<C> storeType( Class<C> storeCls )
    {

        if ( RemoteRepository.class.equals( storeCls ) )
        {
            this.types = Collections.singleton( StoreType.remote );
        }
        else if ( HostedRepository.class.equals( storeCls ) )
        {
            this.types = Collections.singleton( StoreType.hosted );
        }
        else
        {
            this.types = Collections.singleton( group );
        }

        return (ArtifactStoreQuery<C>) this;
    }

    @Override
    public ArtifactStoreQuery<T> storeTypes( StoreType... types )
    {
        this.types = new HashSet<>( Arrays.asList( types ) );
        return this;
    }

    @SuppressWarnings( "unused" )
    ArtifactStoreQuery<T> noTypes()
    {
        this.types = Collections.emptySet();
        return this;
    }

    @Override
    public ArtifactStoreQuery<T> concreteStores()
    {
        return storeTypes( StoreType.remote, StoreType.hosted );
    }

    @Override
    public ArtifactStoreQuery<T> enabledState( Boolean enabled )
    {
        this.enabled = enabled;
        return this;
    }

    @Override
    public List<T> getAll()
            throws IndyDataException
    {
        try
        {
            StoreListingDTO<T> listingDTO =
                    client.module( IndyStoreQueryClientModule.class ).getAllStores( packageType, types, enabled );
            return listingDTO == null ? Collections.emptyList() : listingDTO.getItems();
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException(
                    "Failed to get all artifact stores with conditions: packageType: %s, types: %s, enabled: %s", e,
                    packageType, types, enabled );
        }

    }

    @Override
    @Deprecated
    public List<T> getAll( Predicate<ArtifactStore> filter )
            throws IndyDataException
    {
        return getAll().stream().filter( filter ).collect( Collectors.toList() );
    }

    @Override
    public List<T> getAllByDefaultPackageTypes()
            throws IndyDataException
    {
        try
        {
            StoreListingDTO<T> listingDTO = client.module( IndyStoreQueryClientModule.class ).getAllByDefaultPkgTypes();
            return listingDTO.getItems();
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get all artifact stores by default package types.", e );
        }
    }

    @Override
    public T getByName( final String name )
            throws IndyDataException
    {
        try
        {
            return client.module( IndyStoreQueryClientModule.class ).getByName( name );
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get store by name %s.", e, name );
        }
    }

    @Override
    public Set<Group> getGroupsContaining( final StoreKey storeKey )
            throws IndyDataException
    {
        try
        {
            return new HashSet<>( client.module( IndyStoreQueryClientModule.class )
                                        .getGroupContaining( storeKey, "true" )
                                        .getItems() );
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get groups contains %s.", e, storeKey );
        }
    }

    @Override
    public Set<Group> getGroupsContaining( StoreKey storeKey, Boolean enabled )
            throws IndyDataException
    {
        try
        {
            StoreListingDTO<Group> groups = client.module( IndyStoreQueryClientModule.class )
                                                  .getGroupContaining( storeKey, enabled.toString() );
            return groups == null ? Collections.emptySet() : new HashSet<>( groups.getItems() );
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get groups contains %s.", e, storeKey );
        }
    }

    @Override
    public List<RemoteRepository> getRemoteRepositoryByUrl( String packageType, String url )
            throws IndyDataException
    {
        return getRemoteRepositoryByUrl( packageType, url, true );
    }

    @Override
    public RemoteRepository getRemoteRepository( String packageType, String name )
            throws IndyDataException
    {
        return (RemoteRepository) dataManager.getArtifactStore( new StoreKey( packageType, StoreType.remote, name ) );
    }

    @Override
    public List<RemoteRepository> getRemoteRepositoryByUrl( String packageType, String url, Boolean enabled )
            throws IndyDataException
    {
        try
        {
            StoreListingDTO<RemoteRepository> dto = client.module( IndyStoreQueryClientModule.class )
                                                          .getRemoteRepositoryByUrl( packageType, url,
                                                                                     enabled.toString() );
            if ( dto != null )
            {
                return dto.getItems();
            }
            return Collections.emptyList();
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get remotes for package type %s by url %s", e, packageType, url );
        }
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( String packageType, String groupName )
            throws IndyDataException
    {
        return getOrderedConcreteStoresInGroup( packageType, groupName, Boolean.TRUE );
    }

    @Override
    public List<ArtifactStore> getOrderedConcreteStoresInGroup( String packageType, String groupName, Boolean enabled )
            throws IndyDataException
    {
        final AtomicReference<IndyDataException> eHolder = new AtomicReference<>();
        final String queryKey =
                String.format( "%s:%s:%s:%s", packageType, groupName, enabled, "orderedConcreteStoresInGroup" );
        final Collection<ArtifactStore> stores = computeIfAbsent( queryKey, () -> {
            try
            {
                StoreListingDTO<ArtifactStore> dto = client.module( IndyStoreQueryClientModule.class )
                                                           .getOrderedConcreteStoresInGroup( packageType, groupName,
                                                                                             enabled.toString() );
                if ( dto != null )
                {
                    return dto.getItems();
                }
                return Collections.emptyList();
            }
            catch ( IndyClientException e )
            {
                eHolder.set( new IndyDataException( "Failed to get ordered concrete stores for group: %s:group:%s", e,
                                                    packageType, groupName ) );
                return null;
            }
        }, STORE_QUERY_EXPIRATION_IN_MINS, Boolean.FALSE );

        if ( eHolder.get() != null )
        {
            logger.error( eHolder.get().getMessage() );
            throw eHolder.get();
        }
        return new ArrayList<>( stores );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( String packageType, String groupName )
            throws IndyDataException
    {
        return getOrderedStoresInGroup( packageType, groupName, Boolean.TRUE );
    }

    @Override
    public List<ArtifactStore> getOrderedStoresInGroup( String packageType, String groupName, Boolean enabled )
            throws IndyDataException
    {
        try
        {
            StoreListingDTO<ArtifactStore> dto = client.module( IndyStoreQueryClientModule.class )
                                                       .getOrderedStoresInGroup( packageType, groupName,
                                                                                 enabled.toString() );
            if ( dto != null )
            {
                return dto.getItems();
            }
            return Collections.emptyList();
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get ordered stores for group: %s:group:%s", e, packageType,
                                         groupName );
        }
    }

    @Override
    public Set<Group> getGroupsAffectedBy( StoreKey... keys )
            throws IndyDataException
    {
        return getGroupsAffectedBy( Arrays.stream( keys ).collect( Collectors.toSet() ) );
    }

    @Override
    public Set<Group> getGroupsAffectedBy( Collection<StoreKey> keys )
            throws IndyDataException
    {
        final AtomicReference<IndyDataException> eHolder = new AtomicReference<>();
        final Set<StoreKey> queryKeys = new HashSet<>( keys );
        Collection<ArtifactStore> stores = computeIfAbsent( queryKeys, () -> {
            try
            {
                logger.trace( "Get StoreListingDTO from store query client." );
                StoreListingDTO<Group> dto =
                        client.module( IndyStoreQueryClientModule.class ).getGroupsAffectedBy( new HashSet<>( keys ) );
                if ( dto != null )
                {
                    logger.trace( "StoreListingDTO {}", dto );
                    return new HashSet<>( dto.getItems() );
                }
                return Collections.emptySet();
            }
            catch ( IndyClientException e )
            {
                eHolder.set( new IndyDataException( "Failed to get groups affected by %s", e, keys ) );
                return null;
            }
        }, STORE_QUERY_EXPIRATION_IN_MINS, false );

        if ( eHolder.get() != null )
        {
            logger.error( eHolder.get().getMessage() );
            throw eHolder.get();
        }

        return stores.stream().map( s -> (Group) s ).collect( Collectors.toSet() );
    }

    @Override
    public HostedRepository getHostedRepository( String packageType, String name )
            throws IndyDataException
    {
        return (HostedRepository) dataManager.getArtifactStore( new StoreKey( packageType, StoreType.hosted, name ) );
    }

    @Override
    public Group getGroup( String packageType, String name )
            throws IndyDataException
    {
        return (Group) dataManager.getArtifactStore( new StoreKey( packageType, StoreType.group, name ) );
    }

    @Override
    public ArtifactStoreQuery<T> noPackageType()
    {
        this.packageType = null;
        return this;
    }

    ArtifactStoreQuery<T> packageType( final String packageType )
    {
        if ( packageType == null )
        {
            this.noPackageType();
        }
        else if ( PackageTypeConstants.isValidPackageType( packageType ) )
        {
            this.packageType = packageType;
        }

        return this;
    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories( final String packageType )
            throws IndyDataException
    {
        return getAllRemoteRepositories( packageType, true );

    }

    @Override
    public List<RemoteRepository> getAllRemoteRepositories( final String packageType, Boolean enabled )
            throws IndyDataException
    {
        try
        {
            StoreListingDTO<RemoteRepository> dto = client.module( IndyStoreQueryClientModule.class )
                                                          .getAllRemoteRepositories( packageType, enabled.toString() );
            if ( dto != null )
            {
                return dto.getItems();
            }
            return Collections.emptyList();
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get all remote repositories for package type %s with enabled %s", e,
                                         packageType, enabled );
        }
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories( String packageType )
            throws IndyDataException
    {
        return getAllHostedRepositories( packageType, true );
    }

    @Override
    public List<HostedRepository> getAllHostedRepositories( String packageType, Boolean enabled )
            throws IndyDataException
    {
        try
        {
            StoreListingDTO<HostedRepository> dto = client.module( IndyStoreQueryClientModule.class )
                                                          .getAllHostedRepositories( packageType, enabled.toString() );
            if ( dto != null )
            {
                return dto.getItems();
            }
            return Collections.emptyList();
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get all hosted repositories for package type %s with enabled %s", e,
                                         packageType, enabled );
        }
    }

    @Override
    public List<Group> getAllGroups( String packageType )
            throws IndyDataException
    {
        return getAllGroups( packageType, true );
    }

    @Override
    public List<Group> getAllGroups( String packageType, Boolean enabled )
            throws IndyDataException
    {
        try
        {
            StoreListingDTO<Group> dto =
                    client.module( IndyStoreQueryClientModule.class ).getAllGroups( packageType, enabled.toString() );
            if ( dto != null )
            {
                return dto.getItems();
            }
            return Collections.emptyList();
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get all groups for package type %s with enabled %s", e, packageType,
                                         enabled );
        }
    }

    public Boolean isEmpty()
            throws IndyDataException
    {
        try
        {
            SimpleBooleanResultDTO resultDTO = client.module( IndyStoreQueryClientModule.class ).getStoreEmptyResult();
            return resultDTO.getResult();
        }
        catch ( IndyClientException e )
        {
            throw new IndyDataException( "Failed to get the result of the indy store data empty", e );
        }
    }

    private Collection<ArtifactStore> computeIfAbsent( Object key, Supplier<Collection<ArtifactStore>> storeProvider,
                                                       int expirationMins, boolean forceQuery )
    {
        logger.debug( "computeIfAbsent, cache: {}, key: {}", ARTIFACT_STORE_QUERY, key );

        BasicCacheHandle<Object, Collection<ArtifactStore>> cache = cacheProducer.getBasicCache( ARTIFACT_STORE_QUERY );
        Collection<ArtifactStore> stores = cache.get( key );
        if ( stores == null || forceQuery )
        {
            logger.trace( "Entry not found, run put, expirationMins: {}", expirationMins );

            stores = storeProvider.get();

            if ( stores != null )
            {
                if ( expirationMins > 0 )
                {
                    cache.put( key, stores, expirationMins, TimeUnit.MINUTES );
                }
                else
                {
                    cache.put( key, stores );
                }
            }
        }

        logger.trace( "Return value, cache: {}, key: {}, ret: {}", ARTIFACT_STORE_QUERY, key, stores );
        return stores;
    }

}
