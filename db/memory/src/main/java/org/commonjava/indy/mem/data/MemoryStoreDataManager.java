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
package org.commonjava.indy.mem.data;

import org.commonjava.cdi.util.weft.Locker;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.NoOpStoreEventDispatcher;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.commonjava.indy.measure.annotation.MetricNamed.DEFAULT;
import static org.commonjava.indy.model.core.StoreType.hosted;

@ApplicationScoped
@Alternative
public class MemoryStoreDataManager
        implements StoreDataManager
{
    private static final long LOCK_TIMEOUT_SECONDS = 10;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Map<StoreKey, ArtifactStore> stores = new ConcurrentHashMap<>();

    // no need to inject since this is only used internally.
    private final Locker<StoreKey> opLocks = new Locker<>();

    @Inject
    private StoreEventDispatcher dispatcher;

    @Inject
    private IndyConfiguration config;

    protected MemoryStoreDataManager()
    {
    }

    public MemoryStoreDataManager( final boolean unitTestUsage )
    {
        this.dispatcher = new NoOpStoreEventDispatcher();
        this.config = new DefaultIndyConfiguration();
    }

    public MemoryStoreDataManager( final StoreEventDispatcher dispatcher, final IndyConfiguration config )
    {
        this.dispatcher = dispatcher;
        this.config = config;
    }

    @Override
    public ArtifactStoreQuery<ArtifactStore> query()
    {
        logger.debug( "Creating query for data manager: {}", this );
        return new MemoryArtifactStoreQuery<>( this );
    }

    @Override
    public ArtifactStore getArtifactStore( final StoreKey key )
            throws IndyDataException
    {
        return stores.get( key );
    }

    @Override
    @Measure
    public boolean storeArtifactStore( final ArtifactStore store, final ChangeSummary summary,
                                       final boolean skipIfExists, final boolean fireEvents,
                                       final EventMetadata eventMetadata )
            throws IndyDataException
    {
        return store( store, summary, skipIfExists, fireEvents, eventMetadata );
    }

    protected void preStore( final ArtifactStore store, final ArtifactStore original, final ChangeSummary summary,
                             final boolean exists, final boolean fireEvents, final EventMetadata eventMetadata )
            throws IndyDataException
    {
        if ( dispatcher != null && isStarted() && fireEvents )
        {
            logger.debug( "Firing store pre-update event for: {} (originally: {})", store, original );
            dispatcher.updating( exists ? ArtifactStoreUpdateType.UPDATE : ArtifactStoreUpdateType.ADD, eventMetadata,
                                 Collections.singletonMap( store, original ) );

            if ( exists )
            {
                if ( store.isDisabled() && !original.isDisabled() )
                {
                    dispatcher.disabling( eventMetadata, store );
                }
                else if ( !store.isDisabled() && original.isDisabled() )
                {
                    dispatcher.enabling( eventMetadata, store );
                }
            }
        }
    }

    protected void postStore( final ArtifactStore store, final ArtifactStore original, final ChangeSummary summary,
                              final boolean exists, final boolean fireEvents, final EventMetadata eventMetadata )
            throws IndyDataException
    {
        if ( dispatcher != null && isStarted() && fireEvents )
        {
            logger.debug( "Firing store post-update event for: {} (originally: {})", store, original );
            dispatcher.updated( exists ? ArtifactStoreUpdateType.UPDATE : ArtifactStoreUpdateType.ADD, eventMetadata,
                                Collections.singletonMap( store, original ) );

            if ( exists )
            {
                if ( store.isDisabled() && !original.isDisabled() )
                {
                    dispatcher.disabled( eventMetadata, store );
                }
                else if ( !store.isDisabled() && original.isDisabled() )
                {
                    dispatcher.enabled( eventMetadata, store );
                }
            }
        }
    }

    protected void preDelete( final ArtifactStore store, final ChangeSummary summary, final boolean fireEvents,
                              final EventMetadata eventMetadata )
            throws IndyDataException
    {
        if ( dispatcher != null && isStarted() && fireEvents )
        {
            dispatcher.deleting( eventMetadata, store );
        }
    }

    protected void postDelete( final ArtifactStore store, final ChangeSummary summary, final boolean fireEvents,
                               final EventMetadata eventMetadata )
            throws IndyDataException
    {
        if ( dispatcher != null && isStarted() && fireEvents )
        {
            dispatcher.deleted( eventMetadata, store );
        }
    }

    @Override
    public void deleteArtifactStore( final StoreKey key, final ChangeSummary summary,
                                     final EventMetadata eventMetadata )
            throws IndyDataException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        AtomicReference<IndyDataException> error = new AtomicReference<>();
        opLocks.lockAnd( key, LOCK_TIMEOUT_SECONDS, k->{
            try
            {
                final ArtifactStore store = stores.get( key );
                if ( store == null )
                {
                    logger.warn( "No store found for: {}", key );
                    return null;
                }

                if ( isReadonly( store ) )
                {
                    throw new IndyDataException( ApplicationStatus.METHOD_NOT_ALLOWED.code(),
                                                 "The store {} is readonly. If you want to delete this store, please modify it to non-readonly",
                                                 store.getKey() );
                }

                preDelete( store, summary, true, eventMetadata );

                ArtifactStore removed = stores.remove( key );
                logger.info( "REMOVED store: {}", removed );

                postDelete( store, summary, true, eventMetadata );
            }
            catch ( IndyDataException e )
            {
                error.set( e );
            }

            return null;
        }, (k,lock)->{
            error.set( new IndyDataException( "Failed to lock: %s for DELETE after %d seconds.", key,
                                              LOCK_TIMEOUT_SECONDS ) );
            return false;
        } );

        IndyDataException ex = error.get();
        if ( ex != null )
        {
            throw ex;
        }
    }

    @Override
    public boolean isReadonly( final ArtifactStore store )
    {

        if ( store != null )
        {
            if ( store.getKey().getType() == hosted && ( (HostedRepository) store ).isReadonly() )
            {
                return true;
            }
            //TODO: currently we only support to check hosted readonly here, to make the hosted has ability to prevent from
            //      unexpected removing of both files and repo itself. This method may be expand to other repos like remote
            //      or group in the future to support some other functions, like remote repo's "deploy-through"
        }
        return false;
    }

    @Override
    public boolean isReadonly( final StoreKey storeKey )
    {
        return isReadonly( stores.get( storeKey ) );
    }

    @Override
    public void install()
            throws IndyDataException
    {
    }

    @Override
    public void clear( final ChangeSummary summary )
            throws IndyDataException
    {
        stores.clear();
    }

    @Override
    public Set<ArtifactStore> getAllArtifactStores()
            throws IndyDataException
    {
        return new HashSet<>( stores.values() );
    }

    @Override
    public Stream<ArtifactStore> streamArtifactStores()
            throws IndyDataException
    {
        return getAllArtifactStores().stream();
    }

    @Override
    public Map<StoreKey, ArtifactStore> getArtifactStoresByKey()
    {
        return new HashMap<>( stores );
    }

    @Override
    public boolean hasArtifactStore( final StoreKey key )
    {
        return stores.containsKey( key );
    }

    @Override
    public void reload()
            throws IndyDataException
    {
    }

    @Override
    public boolean isStarted()
    {
        return true;
    }

    private boolean store( final ArtifactStore store, final ChangeSummary summary, final boolean skipIfExists,
                           final boolean fireEvents, final EventMetadata eventMetadata )
            throws IndyDataException
    {
        AtomicReference<IndyDataException> error = new AtomicReference<>();
        logger.trace( "Storing {} using operation lock: {}", store, opLocks );
        if ( store == null )
        {
            logger.warn( "Tried to store null ArtifactStore!" );
            return false;
        }

        final StoreKey storeKey = store.getKey();

        Function<StoreKey, Boolean> lockHandler = k -> {
            try
            {
                ArtifactStore original = stores.get( storeKey );
                if ( original == store )
                {
                    // if they're the same instance, warn that preUpdate events may not work correctly!
                    logger.warn(
                            "Storing changes on existing instance of: {}! You forgot to call {}.copyOf(), so preUpdate events may not accurately reflect before/after differences for this change!",
                            store, store.getClass().getSimpleName() );
                }

                if ( !skipIfExists || original == null )
                {
                    try
                    {
                        logger.debug( "Starting pre-store actions for update of: {}", storeKey );
                        preStore( store, original, summary, original != null, fireEvents, eventMetadata );
                        logger.debug( "Pre-store actions complete for update of: {}", storeKey );
                    }
                    catch ( IndyDataException e )
                    {
                        error.set( e );
                        return false;
                    }

                    logger.debug( "Updating {} in ArtifactStore map", storeKey );
                    final ArtifactStore old = stores.put( store.getKey(), store );
                    logger.debug( "Updated {} in ArtifactStore map", storeKey );

                    try
                    {
                        logger.debug( "Starting post-store actions for update of: {}", storeKey );
                        postStore( store, original, summary, original != null, fireEvents, eventMetadata );
                        logger.debug( "Post-store actions complete for update of: {}", storeKey );
                        return true;
                    }
                    catch ( final IndyDataException e )
                    {
                        if ( old != null )
                        {
                            logger.error( "postStore() failed for: {}. Rolling back to old value: {}", store, old );
                            stores.put( old.getKey(), old );
                        }
                    }
                }
            }
            catch ( RuntimeException e )
            {
                logger.error( "Runtime exception trying to store: " + storeKey, e );
            }

            return false;
        };

        BiFunction<StoreKey, ReentrantLock, Boolean> lockFailedHandler = (k,lock)->{
            error.set( new IndyDataException( "Failed to lock: %s for STORE after %d seconds.", k,
                                              LOCK_TIMEOUT_SECONDS ) );
            return false;
        };

        //FIXME: Something tricky happened for opLocks.lockAnd(...) which cause NPE, so here I wrapped it and just return false
        //       and allow following action can go on. When find the root cause for this NPE, will think more about this code.
        if ( opLocks == null || store == null || store.getKey() == null || lockHandler == null || lockFailedHandler == null )
        {
            logger.warn( "Before locking, opLocks is {} || store is {} || store.getKey is {}", opLocks, store,
                         storeKey );
        }
        boolean result;
        try
        {
            result = opLocks.lockAnd( storeKey, LOCK_TIMEOUT_SECONDS, lockHandler, lockFailedHandler );
        }
        catch ( NullPointerException e )
        {
            logger.error(String.format( "Some unknown errors happened when storing store %s", store), e);
            return false;
        }

        IndyDataException ex = error.get();
        if ( ex != null )
        {
            throw ex;
        }

        return result;
    }

}
