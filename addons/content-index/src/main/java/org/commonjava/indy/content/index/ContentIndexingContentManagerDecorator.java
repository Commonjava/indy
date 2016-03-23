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
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
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
import javax.enterprise.inject.Any;
import javax.inject.Inject;
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

    @ExecutorConfig( named = "content-indexer", threads = 8, priority = 2, daemon = true )
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
        Transfer transfer = null;
        for ( ArtifactStore store : stores )
        {
            transfer = retrieve( store, path, eventMetadata );
            if ( transfer != null )
            {
                break;
            }
        }

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
        List<Transfer> results = new ArrayList<>();
        stores.stream().map( ( store ) -> {
            try
            {
                return retrieve( store, path, eventMetadata );
            }
            catch ( IndyWorkflowException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error(
                        String.format( "Failed to retrieve indexed content: %s:%s. Reason: %s", store.getKey(),
                                       path, e.getMessage() ), e );
            }

            return null;
        } ).filter((transfer)->transfer != null).forEachOrdered( ( transfer ) -> {
            if ( transfer != null )
            {
                results.add( transfer );
            }
        } );

        return results;
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
        Logger logger = LoggerFactory.getLogger( getClass() );

        Transfer transfer = getIndexedTransfer( store.getKey(), path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            return transfer;
        }

        if ( StoreType.group == store.getKey().getType() )
        {
            logger.debug( "No group index hits. Devolving to member store indexes." );
            for ( StoreKey key : ( (Group) store ).getConstituents() )
            {
                transfer = getIndexedTransfer( key, path, TransferOperation.DOWNLOAD );
                if ( transfer != null )
                {
                    indexTransferIn( store.getKey(), transfer );
                    return transfer;
                }
            }
        }

        logger.debug( "No index hits. Delegating to main content manager for: {} in: {}", path, store );
        transfer = delegate.retrieve( store, path, eventMetadata );

        if ( transfer != null )
        {
            logger.debug( "Got transfer from delegate: {} (will index)", transfer );

            indexTransferIn( LocationUtils.getKey( transfer ), transfer );
            indexTransferIn( store.getKey(), transfer );
        }

        logger.debug( "Returning transfer: {}", transfer );
        return transfer;
    }

    private List<IndexedStorePath> getByTopKey( StoreKey key, String path )
    {
        QueryFactory queryFactory = Search.getQueryFactory( contentIndex );
        QueryBuilder<Query> queryBuilder = queryFactory.from( IndexedStorePath.class )
                                                       .having( "storeType" )
                                                       .eq( key.getType() )
                                                       .and()
                                                       .having( "storeName" )
                                                       .eq( key.getName() )
                                                       .and()
                                                       .having( "path" )
                                                       .eq( path )
                                                       .toBuilder();

        return queryBuilder.build().list();
    }

    @Override
    public Transfer getTransfer( ArtifactStore store, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        Transfer transfer = getIndexedTransfer( store.getKey(), path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            return transfer;
        }

        if ( StoreType.group == store.getKey().getType() )
        {
            logger.debug( "No group index hits. Devolving to member store indexes." );
            for ( StoreKey key : ( (Group) store ).getConstituents() )
            {
                transfer = getIndexedTransfer( key, path, TransferOperation.DOWNLOAD );
                if ( transfer != null )
                {
                    indexTransferIn( store.getKey(), transfer );
                    return transfer;
                }
            }
        }

        transfer = delegate.getTransfer( store, path, op );
        if ( transfer != null )
        {
            indexTransferIn( LocationUtils.getKey( transfer ), transfer );
            indexTransferIn( store.getKey(), transfer );
        }

        return transfer;
    }

    @Override
    public Transfer getTransfer( StoreKey storeKey, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        Transfer transfer = getIndexedTransfer( storeKey, path, TransferOperation.DOWNLOAD );
        if ( transfer != null )
        {
            return transfer;
        }

        if ( StoreType.group == storeKey.getType() )
        {
            logger.debug( "No group index hits. Devolving to member store indexes." );
            try
            {
                Group g = storeDataManager.getGroup( storeKey.getName() );

                for ( StoreKey key : g.getConstituents() )
                {
                    transfer = getIndexedTransfer( key, path, TransferOperation.DOWNLOAD );
                    if ( transfer != null )
                    {
                        indexTransferIn( storeKey, transfer );
                        return transfer;
                    }
                }
            }
            catch ( IndyDataException e )
            {
                logger.error(
                        String.format( "Cannot lookup ArtifactStore for key: %s. Reason: %s", storeKey, e.getMessage() ),
                        e );
            }
        }

        transfer = delegate.getTransfer( storeKey, path, op );
        if ( transfer != null )
        {
            indexTransferIn( LocationUtils.getKey( transfer ), transfer );
            indexTransferIn( storeKey, transfer );
        }

        return transfer;
    }

    @Override
    public Transfer getTransfer( List<ArtifactStore> stores, String path, TransferOperation op )
            throws IndyWorkflowException
    {
        Transfer transfer = null;
        for ( ArtifactStore store : stores )
        {
            transfer = getTransfer( store, path, op );
            if ( transfer != null )
            {
                break;
            }
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
            indexTransferIn( LocationUtils.getKey( transfer ), transfer );
            indexTransferIn( store.getKey(), transfer );
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
            indexTransferIn( LocationUtils.getKey( transfer ), transfer );
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

    private Transfer getIndexedTransfer( StoreKey key, String path, TransferOperation operation )
            throws IndyWorkflowException
    {
        List<IndexedStorePath> matches = getByTopKey( key, path );
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Found index hits for: {} in store: {}:\n  {}", path, key, matches );

        if ( !matches.isEmpty() )
        {
            IndexedStorePath storePath = findFirstMatch( matches, key );
            logger.debug( "Selected match: {}", storePath );

            Transfer result = delegate.getTransfer( storePath.getOriginStoreKey(), path, operation );

            logger.debug( "Returning transfer for indexed result: {}", result );
            return result;
        }

        return null;
    }

    /**
     * When we store or retrieve content, index it for faster reference next time.
     */
    private void indexTransferIn( StoreKey key, Transfer transfer )
    {
        if ( transfer != null && transfer.exists() )
        {
            executor.execute( () -> {
                String path = transfer.getPath();

                IndexedStorePath isp = new IndexedStorePath( key, key, path );
                contentIndex.put( isp, isp );

                indexTransferInGroupsOf( key, key, path );
            } );
        }
    }

    /**
     * When we index content, also index it for groups containing its origin stores, to make indexes more efficient.
     * This will be called recursively for groups of groups.
     */
    private void indexTransferInGroupsOf( StoreKey key, StoreKey originKey, String path )
    {
//        try
//        {
//            Set<Group> groups = storeDataManager.getGroupsContaining( key );
//            if ( groups != null )
//            {
//                new HashSet<>( groups ).forEach( ( group ) -> {
//                    IndexedStorePath sp = new IndexedStorePath( group.getKey(), originKey, path );
//                    contentIndex.put( sp, sp );
//
//                    // denormalize to groups of groups
//                    indexTransferInGroupsOf( group.getKey(), originKey, path );
//                } );
//            }
//        }
//        catch ( IndyDataException e )
//        {
//            Logger logger = LoggerFactory.getLogger( getClass() );
//            logger.error( String.format( "Cannot lookup groups containing: %s for content indexing. Reason: %s", key,
//                                         e.getMessage() ), e );
//        }
    }

    /**
     * This method is sort of stupid, but I'm trying not to retrieve any more store data than I need. I'm checking if
     * there is an indexed store path that matches:
     * <ol>
     *     <li>The current store (the only option if it's not a group)</li>
     *     <li>The direct membership of the group (the StoreKey instances already available in the Group constituents list)</li>
     *     <li>The ordered concrete membership of the group, which includes ArtifactStores that are available by recursing down through groups.</li>
     * </ol>
     *
     * If all of that fails, and the matches list isn't empty, that means something somewhere matched...so let's return
     * the first of those. But that really should NEVER happen.
     */
    private IndexedStorePath findFirstMatch( List<IndexedStorePath> matches, StoreKey key )
    {
        if ( matches.isEmpty() )
        {
            return null;
        }

        // we're managing the down-membership indexing for a group aggressively, so we should be able to pop the first
        // result off the list.
        return matches.get(0);

//        if ( StoreType.group != store.getKey().getType() )
//        {
//            return matches.get( 0 );
//        }
//
//        Group group = (Group) store;
//        Optional<IndexedStorePath> result = matches.stream().filter( ( match ) -> {
//            if ( match.getOriginStoreKey().equals( group.getKey() ) )
//            {
//                return true;
//            }
//
//            return false;
//        } ).findAny();
//
//        if ( result.isPresent() )
//        {
//            return result.get();
//        }
//
//        Optional<StoreKey> matchingKey = group.getConstituents().stream().filter( ( memberKey ) -> {
//            Optional<IndexedStorePath> found = matches.stream().filter( ( isp ) -> {
//                if ( isp.getOriginStoreKey().equals( memberKey ) )
//                {
//                    return true;
//                }
//
//                return false;
//            } ).findAny();
//
//            return found.isPresent();
//        } ).findAny();
//
//        StoreKey lookupKey = null;
//        if ( matchingKey.isPresent() )
//        {
//            lookupKey = matchingKey.get();
//        }
//        else
//        {
//            try
//            {
//                Optional<ArtifactStore> matchingStore =
//                        storeDataManager.getOrderedConcreteStoresInGroup( group.getName() )
//                                        .stream()
//                                        .filter( ( member ) -> {
//                                            return matches.stream().filter( ( isp ) -> {
//                                                if ( isp.getOriginStoreKey().equals( member.getKey() ) )
//                                                {
//                                                    return true;
//                                                }
//
//                                                return false;
//                                            } ).findAny().isPresent();
//                                        } )
//                                        .findAny();
//
//                if ( matchingStore.isPresent() )
//                {
//                    lookupKey = matchingStore.get().getKey();
//                }
//            }
//            catch ( IndyDataException e )
//            {
//                Logger logger = LoggerFactory.getLogger( getClass() );
//                logger.error( String.format( "Failed to find ordered concrete stores for group: %s. Reason: %s",
//                                             group.getKey(), e.getMessage() ), e );
//            }
//        }
//
//        if ( lookupKey != null )
//        {
//            StoreKey finalLookupKey = lookupKey;
//            result = matches.stream().filter( ( match ) -> {
//                if ( match.getOriginStoreKey().equals( finalLookupKey ) )
//                {
//                    return true;
//                }
//
//                return false;
//            } ).findAny();
//
//            if ( result.isPresent() )
//            {
//                return result.get();
//            }
//        }
//
//        return matches.get( 0 );
    }

}
