/*
 * Copyright (c) 2022 Red Hat, Inc
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

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.module.IndyStoresClientModule;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.NoOpStoreEventDispatcher;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.db.common.AbstractStoreDataManager;
import org.commonjava.indy.db.common.StoreUpdateAction;
import org.commonjava.indy.db.common.inject.Serviced;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.subsys.service.inject.ServiceClient;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.commonjava.indy.model.core.StoreType.hosted;

@SuppressWarnings( "unchecked" )
@ApplicationScoped
@Serviced
public class ServiceStoreDataManager
        extends AbstractStoreDataManager
        implements StoreDataManager
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    @ServiceClient
    private Indy client;

    @Inject
    private CacheProducer cacheProducer;

    private ServiceStoreQuery<ArtifactStore> serviceStoreQuery;

    final static String ARTIFACT_STORE = "artifact-store";

    private final Integer STORE_EXPIRATION_IN_MINS = 15;

    @SuppressWarnings( "unused" )
    ServiceStoreDataManager()
    {
    }

    ServiceStoreDataManager( final CacheProducer cacheProducer )
    {
        this.cacheProducer = cacheProducer;
    }

    protected ServiceStoreDataManager( final CacheProducer cacheProducer, final Indy client )
    {
        this( cacheProducer );
        this.client = client;
    }

    @Override
    protected StoreEventDispatcher getStoreEventDispatcher()
    {
        // This StoreDataManager will delegate all operations to remote repository service,
        // so no need to care about any store events which should be handled in repository service
        return new NoOpStoreEventDispatcher();
    }

    protected ArtifactStore getArtifactStoreInternal( StoreKey key )
    {
        return doQueryArtifactStoreInternal( key, false );
    }

    protected ArtifactStore getArtifactStoreInternal( StoreKey key, boolean forceQuery )
    {
        return doQueryArtifactStoreInternal( key, forceQuery );
    }

    private ArtifactStore doQueryArtifactStoreInternal( StoreKey key, boolean forceQuery )
    {
        AtomicReference<IndyDataException> eHolder = new AtomicReference<>();
        ArtifactStore store = computeIfAbsent( key, () -> {
            try
            {
                return client.module( IndyStoresClientModule.class ).load( key, key.getType().getStoreClass() );
            }
            catch ( IndyClientException e )
            {
                eHolder.set( new IndyDataException( "Failed to get store %s", e, key ) );
                return null;
            }
        }, STORE_EXPIRATION_IN_MINS, forceQuery );
        if ( eHolder.get() != null )
        {
            logger.error( "Can not get ArtifactStore for {} due to: {}", key, eHolder.get().getMessage() );
            throw new RuntimeException( eHolder.get() );
        }
        return store;
    }

    protected ArtifactStore putArtifactStoreInternal( StoreKey key, ArtifactStore store )
    {
        return computeIfAbsent( key, () -> {
            try
            {
                Class<ArtifactStore> storeCls = (Class<ArtifactStore>) key.getType().getStoreClass();
                client.module( IndyStoresClientModule.class )
                      .create( store, String.format( "Create store %s", key ), storeCls );
                return store;
            }
            catch ( IndyClientException e )
            {
                throw new RuntimeException( e );
            }
        }, STORE_EXPIRATION_IN_MINS, Boolean.TRUE );
    }

    @Override
    public void clear( ChangeSummary summary )
    {
        cacheProducer.getBasicCache( ARTIFACT_STORE ).clear();
        // I don't think we should call this on remote repository service level, so just log here
        logger.warn( "Will not call this clear method on remote repository service for safety consideration." );
    }

    @Override
    public void install()
    {
        //Do nothing: will be handled in repository service.
        logger.warn( "This is controlled by remote repository service and should not be called here!" );
    }

    @Override
    public void reload()
    {
        //Do nothing: will be handled in repository service.
        logger.warn( "This is controlled by remote repository service and should not be called here!" );
    }

    @Override
    public Set<ArtifactStore> getAllArtifactStores()
            throws IndyDataException
    {
        // TODO: Need to check all usage of this method and optimize to use more specific query methods.
        return new HashSet<>( queryInternal().noPackageType().getAll() );
    }

    @Override
    public Stream<ArtifactStore> streamArtifactStores()
            throws IndyDataException
    {
        // TODO: Need to check all usage of this method and optimize to use more specific query methods.
        return getAllArtifactStores().stream();
    }

    @Override
    public Map<StoreKey, ArtifactStore> getArtifactStoresByKey()
    {
        //Not found where this method is used, so not implement and give a warning here
        logger.warn( "Not used anywhere! So should not be called! See below for the calling stack trace" );
        Thread.dumpStack();
        return emptyMap();
    }

    @Override
    protected ArtifactStore removeArtifactStoreInternal( StoreKey key )
    {
        // do nothing as delete action is implemented in override deleteArtifactStore method.
        return null;
    }

    @Override
    public Set<ArtifactStore> getArtifactStoresByPkgAndType( String packageType, StoreType storeType )
    {
        try
        {
            return new HashSet<>( queryInternal().packageType( packageType ).storeTypes( storeType ).getAll() );
        }
        catch ( IndyDataException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public boolean hasArtifactStore( StoreKey key )
    {
        ArtifactStore store;
        try
        {
            store = getArtifactStoreInternal( key );
        }
        catch ( RuntimeException e )
        {
            logger.error( "Cannot get artifact store {} to check existence! error: {}", key, e.getMessage() );
            return false;
        }
        return store != null;
    }

    @Override
    public ArtifactStore getArtifactStore( StoreKey key )
            throws IndyDataException
    {
        try
        {
            return getArtifactStoreInternal( key );
        }
        catch ( RuntimeException e )
        {
            throw new IndyDataException( "Failed to get store %s", e, key );

        }
    }

    public ArtifactStore getArtifactStore( StoreKey key, boolean forceQuery )
                    throws IndyDataException
    {
        try
        {
            return getArtifactStoreInternal( key, forceQuery );
        }
        catch ( RuntimeException e )
        {
            throw new IndyDataException( "Failed to get store %s through forceQuery.", e, key );

        }
    }


    @Override
    public boolean isStarted()
    {
        return true;
    }

    @Override
    public boolean isReadonly( ArtifactStore store )
    {
        return store != null && store.getKey().getType() == hosted && ( (HostedRepository) store ).isReadonly();
    }

    @Override
    public boolean isEmpty()
    {
        try
        {
            return queryInternal().isEmpty();
        }
        catch ( IndyDataException e )
        {
            logger.error( "Can not check if there is repository definitions in remote repository service due to {}",
                          e.getMessage() );
            //TODO: Is it reasonable to return true?
            return true;
        }
    }

    @Override
    @Deprecated
    public Stream<StoreKey> streamArtifactStoreKeys()
    {
        // TODO: Need to check all usage of this method and optimize to use more specific query methods.
        try
        {
            return streamArtifactStores().map( ArtifactStore::getKey );
        }
        catch ( IndyDataException e )
        {
            logger.error( "An error happened when streaming artifact stores to keys: {}", e.getMessage() );
            throw new RuntimeException( e );
        }
    }

    @Override
    @Deprecated
    public Set<StoreKey> getStoreKeysByPkg( String pkg )
    {
        //TODO: seems this method is not used anywhere, so may be removed in future
        try
        {
            List<ArtifactStore> stores = queryInternal().packageType( pkg ).getAll();
            return stores.stream().map( ArtifactStore::getKey ).collect( Collectors.toSet() );
        }
        catch ( IndyDataException e )
        {
            logger.error( "An error happened when get store keys by pkg: {}", e.getMessage() );
            throw new RuntimeException( e );
        }
    }

    @Override
    public Set<StoreKey> getStoreKeysByPkgAndType( String pkg, StoreType type )
    {
        return getArtifactStoresByPkgAndType( pkg, type ).stream()
                                                         .map( ArtifactStore::getKey )
                                                         .collect( Collectors.toSet() );
    }

    @Override
    public Set<Group> affectedBy( Collection<StoreKey> keys )
            throws IndyDataException
    {
        return queryInternal().getGroupsAffectedBy( keys );
    }

    // Override methods in AbstractStoreDataManager from here

    @Override
    public ArtifactStoreQuery<ArtifactStore> query()
    {
        return new ServiceStoreQuery<>( this, this.cacheProducer );
    }

    // This method is a replacement of the query() for internal usage of this class to avoid
    // duplicated objects creation
    private synchronized ServiceStoreQuery<ArtifactStore> queryInternal()
    {
        if ( this.serviceStoreQuery == null )
        {
            this.serviceStoreQuery = new ServiceStoreQuery<>( this, this.cacheProducer );
        }
        return this.serviceStoreQuery;
    }

    @Override
    public void deleteArtifactStore( final StoreKey key, final ChangeSummary summary,
                                     final EventMetadata eventMetadata )
            throws IndyDataException
    {
        ArtifactStore store = null;
        try
        {
            store = getArtifactStoreInternal( key );
        }
        catch ( RuntimeException e )
        {
            logger.error( "Cannot get ArtifactStore {} due to: {}", key, e.getMessage() );
        }

        if ( store == null )
        {
            logger.warn( "No store found for: {}", key );
            return;
        }

        if ( isReadonly( store ) )
        {
            throw new IndyDataException( ApplicationStatus.METHOD_NOT_ALLOWED.code(),
                                         "The store {} is readonly. If you want to delete this store, please modify it to non-readonly",
                                         key );
        }

        try
        {
            client.module( IndyStoresClientModule.class ).delete( key, String.format( "Remove store %s", key ) );
        }
        catch ( IndyClientException e )
        {
            logger.error( "Cannot delete ArtifactStore {} due to: {}", key, e.getMessage() );
        }
    }

    @Override
    public Set<Group> filterAffectedGroups( Set<Group> affectedGroups )
    {
        if ( affectedGroups == null )
        {
            return emptySet();
        }

        //TODO: wait for implementation
        return emptySet();
    }

    protected Indy getIndyClient()
    {
        return this.client;
    }

    private ArtifactStore computeIfAbsent( StoreKey key, Supplier<ArtifactStore> storeProvider, int expirationMins,
                                           boolean forceQuery )
    {
        logger.debug( "computeIfAbsent, cache: {}, key: {}", ARTIFACT_STORE, key );

        BasicCacheHandle<StoreKey, ArtifactStore> cache = cacheProducer.getBasicCache( ARTIFACT_STORE );
        ArtifactStore store = cache.get( key );
        if ( store == null || forceQuery )
        {
            logger.trace( "Entry not found, run put, expirationMins: {}", expirationMins );

            store = storeProvider.get();

            if ( store != null )
            {
                if ( expirationMins > 0 )
                {
                    cache.put( key, store, expirationMins, TimeUnit.MINUTES );
                }
                else
                {
                    cache.put( key, store );
                }
            }
        }

        logger.trace( "Return value, cache: {}, key: {}, ret: {}", ARTIFACT_STORE, key, store );
        return store;
    }

    @Override
    protected void refreshAffectedBy( final ArtifactStore store, final ArtifactStore original,
                                      StoreUpdateAction action )
    {
        // do nothing
        logger.debug( "Do nothing here. Delegate to repository service for further operations." );
    }

    @Override
    protected void removeAffectedBy( StoreKey key, StoreKey affected )
    {
        // do nothing
        logger.debug( "Do nothing here. Delegate to repository service for further operations." );
    }

    @Override
    protected void addAffectedBy( StoreKey key, StoreKey affected )
    {
        // do nothing
        logger.debug( "Do nothing here. Delegate to repository service for further operations." );
    }

    @Override
    protected void removeAffectedStore( StoreKey key )
    {
        // do nothing
        logger.debug( "Do nothing here. Delegate to repository service for further operations." );
    }

}
