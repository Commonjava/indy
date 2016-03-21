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
package org.commonjava.indy.content.index;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.core.expire.ContentExpiration;
import org.commonjava.indy.core.expire.ScheduleManager;
import org.commonjava.indy.core.expire.SchedulerEvent;
import org.commonjava.indy.core.expire.SchedulerEventType;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.infinispan.Cache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Decorator for ContentManager which uses Infinispan to index content to avoid having to iterate all members of large
 * groups looking for a file.
 *
 * Created by jdcasey on 3/15/16.
 */
@Decorator
public abstract class ContentIndexingContentManagerDecorator
        implements ContentManager
{
    @Inject
    private StoreDataManager storeDataManager;

    @Delegate
    @Any
    @Inject
    private ContentManager delegate;

    @ConfigureCache( "content-index" )
    @Inject
    private Cache<IndexedStorePath, IndexedStorePath> contentIndex;

    @ExecutorConfig( named = "content-indexer", threads = 4, daemon = true )
    @WeftManaged
    @Inject
    private Executor executor;

    protected ContentIndexingContentManagerDecorator()
    {
    }

    protected ContentIndexingContentManagerDecorator( StoreDataManager storeDataManager, ContentManager delegate,
                                                      Cache<IndexedStorePath, IndexedStorePath> contentIndex,
                                                      Executor executor )
    {
        this.storeDataManager = storeDataManager;
        this.delegate = delegate;
        this.contentIndex = contentIndex;
        this.executor = executor;
    }

    @Override
    public Transfer retrieveFirst( List<? extends ArtifactStore> stores, String path )
            throws IndyWorkflowException
    {
        return retrieveFirst( stores, path, new EventMetadata() );
    }

    @Override
    public Transfer retrieveFirst( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Transfer transfer = getIndexedTransfer( stores, path, TransferOperation.DOWNLOAD );

        if ( transfer != null )
        {
            return transfer;
        }

        transfer = delegate.retrieveFirst( stores, path, eventMetadata );
        indexTransfer( transfer );

        return transfer;
    }

    @Override
    public List<Transfer> retrieveAll( List<? extends ArtifactStore> stores, String path )
            throws IndyWorkflowException
    {
        return retrieveAll( stores, path, new EventMetadata() );
    }

    @Override
    public List<Transfer> retrieveAll( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        List<Transfer> indexResults = new ArrayList<>();
        stores.stream().map( ( store ) -> {
            if ( findByTopKey( store.getKey(), path ) )
            {
                try
                {
                    return delegate.getTransfer( store, path, TransferOperation.DOWNLOAD );
                }
                catch ( IndyWorkflowException e )
                {
                    Logger logger = LoggerFactory.getLogger( getClass() );
                    logger.error(
                            String.format( "Failed to retrieve indexed content: %s:%s. Reason: %s", store.getKey(),
                                           path, e.getMessage() ), e );
                }
            }

            return null;
        } ).forEachOrdered( ( transfer ) -> {
            if ( transfer != null )
            {
                indexResults.add( transfer );
            }
        } );

        if ( !indexResults.isEmpty() )
        {
            return indexResults;
        }

        List<Transfer> transfers = delegate.retrieveAll( stores, path, eventMetadata );
        if ( transfers != null && !transfers.isEmpty() )
        {
            executor.execute( () -> {
                transfers.forEach( ( transfer ) -> {
                    StoreKey key = LocationUtils.getKey( transfer );
                    IndexedStorePath isp = new IndexedStorePath( key, key, path );
                    contentIndex.put( isp, isp );

                    try
                    {
                        Set<Group> groups = storeDataManager.getGroupsContaining( key );
                        if ( groups != null )
                        {
                            groups.forEach( ( group ) -> {
                                IndexedStorePath sp = new IndexedStorePath( group.getKey(), key, path );
                                contentIndex.put( sp, sp );
                            } );
                        }
                    }
                    catch ( IndyDataException e )
                    {
                        Logger logger = LoggerFactory.getLogger( getClass() );
                        logger.error(
                                String.format( "Cannot lookup groups containing: %s for content indexing. Reason: %s",
                                               key, e.getMessage() ), e );
                    }
                } );
            } );
        }

        return transfers;
    }

    @Override
    public Transfer retrieve( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        return retrieve( store, path );
    }

    @Override
    public Transfer retrieve( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        if ( findByTopKey( store.getKey(), path ) )
        {
            return delegate.getTransfer( store, path, TransferOperation.DOWNLOAD );
        }

        Transfer transfer = delegate.retrieve( store, path, eventMetadata );
        indexTransfer( transfer );

        return transfer;
    }

    private boolean findByTopKey( StoreKey key, String path )
    {
        QueryFactory queryFactory = Search.getQueryFactory( contentIndex );
        QueryBuilder<Query> queryBuilder = queryFactory.from( IndexedStorePath.class )
                                                       .having( "storeType" )
                                                       .eq( key.getType() )
                                                       .and()
                                                       .having( "storeName" )
                                                       .eq( key.getName() )
                                                       .toBuilder();

        return queryBuilder.build().getResultSize() > 0;
    }

    @Override
    public Transfer getTransfer( ArtifactStore store, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        if ( findByTopKey( store.getKey(), path ) )
        {
            return delegate.getTransfer( store, path, op );
        }

        Transfer transfer = delegate.getTransfer( store, path, op );
        indexTransfer( transfer );

        return transfer;
    }

    @Override
    public Transfer getTransfer( StoreKey storeKey, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        if ( findByTopKey( storeKey, path ) )
        {
            return delegate.getTransfer( storeKey, path, op );
        }

        Transfer transfer = delegate.getTransfer( storeKey, path, op );
        indexTransfer( transfer );

        return transfer;
    }

    @Override
    public Transfer getTransfer( List<ArtifactStore> stores, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        Transfer transfer = getIndexedTransfer( stores, path, op );
        if ( transfer != null )
        {
            return transfer;
        }

        transfer = delegate.getTransfer( stores, path, op );
        if ( transfer != null )
        {
            indexTransfer( transfer );
        }

        return transfer;
    }

    @Override
    public Transfer store( ArtifactStore store, String path, InputStream stream, TransferOperation op )
            throws IndyWorkflowException
    {
        return store( store, path, stream, op, new EventMetadata() );
    }

    @Override
    public Transfer store( ArtifactStore store, String path, InputStream stream, TransferOperation op,
                           EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Transfer transfer = delegate.store( store, path, stream, op, eventMetadata );
        if ( transfer != null )
        {
            indexTransfer( transfer );
        }

        return transfer;
    }

    @Override
    public Transfer store( List<? extends ArtifactStore> stores, String path, InputStream stream, TransferOperation op )
            throws IndyWorkflowException
    {
        return store( stores, path, stream, op, new EventMetadata() );
    }

    @Override
    public Transfer store( List<? extends ArtifactStore> stores, String path, InputStream stream, TransferOperation op,
                           EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        Transfer transfer = delegate.store( stores, path, stream, op, eventMetadata );
        if ( transfer != null )
        {
            indexTransfer( transfer );
        }

        return transfer;
    }

    @Override
    public boolean delete( ArtifactStore store, String path )
            throws IndyWorkflowException
    {
        return delete( store, path, new EventMetadata() );
    }

    @Override
    public boolean delete( ArtifactStore store, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        boolean result = delegate.delete( store, path, eventMetadata );
        if ( result )
        {
            deIndex( store, path );
        }

        return result;
    }

    @Override
    public boolean deleteAll( List<? extends ArtifactStore> stores, String path )
            throws IndyWorkflowException
    {
        return deleteAll( stores, path, new EventMetadata() );
    }

    @Override
    public boolean deleteAll( List<? extends ArtifactStore> stores, String path, EventMetadata eventMetadata )
            throws IndyWorkflowException
    {
        boolean result = false;
        for ( ArtifactStore store : stores )
        {
            result = delete( store, path, eventMetadata ) || result;
        }

        return result;
    }

    private void deIndex( ArtifactStore store, String path )
    {
        executor.execute( () -> {
            StoreKey key = store.getKey();
            contentIndex.remove( new IndexedStorePath( key, key, path ) );

            try
            {
                Set<Group> groups = storeDataManager.getGroupsContaining( key );
                if ( groups != null )
                {
                    groups.forEach( ( group ) -> contentIndex.remove( new IndexedStorePath( group.getKey(), key, path ),
                                                                      Boolean.TRUE ) );
                }
            }
            catch ( IndyDataException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error(
                        String.format( "Cannot lookup groups containing: %s for content indexing. Reason: %s", key,
                                       e.getMessage() ), e );
            }

            if ( StoreType.group == key.getType() )
            {
                try
                {
                    List<ArtifactStore> members = storeDataManager.getOrderedConcreteStoresInGroup( key.getName() );
                    if ( members != null )
                    {
                        members.forEach( ( member ) -> {
                            contentIndex.remove( new IndexedStorePath( key, member.getKey(), path ) );

                            contentIndex.remove( new IndexedStorePath( member.getKey(), member.getKey(), path ) );
                        } );
                    }
                }
                catch ( IndyDataException e )
                {
                    Logger logger = LoggerFactory.getLogger( getClass() );
                    logger.error( String.format(
                            "Cannot lookup concrete membership of group: %s for content indexing. Reason: %s", key,
                            e.getMessage() ), e );
                }
            }
        } );
    }

    private Transfer getIndexedTransfer( List<? extends ArtifactStore> stores, String path, TransferOperation op )
    {
        Optional<Transfer> indexResult = stores.stream().map( ( store ) -> {
            if ( findByTopKey( store.getKey(), path ) )
            {
                try
                {
                    return delegate.getTransfer( store, path, op );
                }
                catch ( IndyWorkflowException e )
                {
                    Logger logger = LoggerFactory.getLogger( getClass() );
                    logger.error(
                            String.format( "Failed to retrieve indexed content: %s:%s. Reason: %s", store.getKey(),
                                           path, e.getMessage() ), e );
                }
            }

            return null;

        } ).findFirst();

        return indexResult.isPresent() ? indexResult.get() : null;
    }

    private void indexTransfer( Transfer transfer )
    {
        if ( transfer != null && transfer.exists() )
        {
            executor.execute( () -> {
                StoreKey key = LocationUtils.getKey( transfer );
                String path = transfer.getPath();

                IndexedStorePath isp = new IndexedStorePath( key, key, path );
                contentIndex.put( isp, isp );

                try
                {
                    Set<Group> groups = storeDataManager.getGroupsContaining( key );
                    if ( groups != null )
                    {
                        groups.forEach( ( group ) -> {
                            IndexedStorePath sp = new IndexedStorePath( group.getKey(), key, path );
                            contentIndex.put( sp, sp );
                        } );
                    }
                }
                catch ( IndyDataException e )
                {
                    Logger logger = LoggerFactory.getLogger( getClass() );
                    logger.error(
                            String.format( "Cannot lookup groups containing: %s for content indexing. Reason: %s", key,
                                           e.getMessage() ), e );
                }
            } );
        }
    }

}
