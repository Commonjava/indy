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
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.inject.qualifer.ContentIndexCache;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.infinispan.Cache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Created by jdcasey on 5/2/16.
 */
@ApplicationScoped
public class ContentIndexManager
{
    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private SpecialPathManager specialPathManager;

    @ConfigureCache( "content-index" )
    @ContentIndexCache
    @Inject
    private Cache<IndexedStorePath, IndexedStorePath> contentIndex;

    @ExecutorConfig( named = "content-indexer", threads = 8, priority = 2, daemon = true )
    @WeftManaged
    @Inject
    private Executor executor;


    public void removeAllOriginIndexedPathsForStore( StoreKey memberKey, Consumer<IndexedStorePath> pathConsumer )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        QueryFactory queryFactory = Search.getQueryFactory( contentIndex );
        QueryBuilder<Query> queryBuilder = queryFactory.from( IndexedStorePath.class )
                                                       .having( "originStoreType" )
                                                       .eq( memberKey.getType() )
                                                       .and()
                                                       .having( "originStoreName" )
                                                       .eq( memberKey.getName() )
                                                       .toBuilder();

        List<IndexedStorePath> paths = queryBuilder.build().list();
        paths.forEach( ( indexedStorePath ) -> {
            logger.debug( "Removing {}", indexedStorePath );
            contentIndex.remove( indexedStorePath );
            if ( pathConsumer != null )
            {
                pathConsumer.accept( indexedStorePath );
            }
        } );
    }

    public List<IndexedStorePath> removeAllIndexedPathsForStore( StoreKey key, Consumer<IndexedStorePath> pathConsumer )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        // invalidate indexes for the store itself
        List<IndexedStorePath> paths = getAllIndexedPathsForStore( key );
        paths.forEach( ( indexedStorePath ) -> {
            logger.debug( "Removing: {}", indexedStorePath );
            contentIndex.remove( indexedStorePath.toString() );
            if ( pathConsumer != null )
            {
                pathConsumer.accept( indexedStorePath );
            }
        } );

        return paths;
    }

    public List<IndexedStorePath> getAllIndexedPathsForStore( StoreKey key )
    {
        QueryFactory queryFactory = Search.getQueryFactory( contentIndex );
        QueryBuilder<Query> queryBuilder = queryFactory.from( IndexedStorePath.class )
                                                       .having( "storeType" )
                                                       .eq( key.getType() )
                                                       .and()
                                                       .having( "storeName" )
                                                       .eq( key.getName() )
                                                       .toBuilder();

        List<IndexedStorePath> paths = queryBuilder.build().list();
        return paths;
    }

    public List<IndexedStorePath> removeOriginIndexedStorePath( String path, StoreKey key, Consumer<IndexedStorePath> pathConsumer )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        QueryFactory queryFactory = Search.getQueryFactory( contentIndex );

        QueryBuilder<Query> queryBuilder = queryFactory.from( IndexedStorePath.class )
                                                       .having( "originStoreType" )
                                                       .eq( key.getType() )
                                                       .and()
                                                       .having( "originStoreName" )
                                                       .eq( key.getName() )
                                                       .and()
                                                       .having( "path" )
                                                       .eq( path )
                                                       .toBuilder();

        List<IndexedStorePath> paths = queryBuilder.build().list();
        paths.forEach( ( indexedStorePath ) -> {
            logger.debug( "Removing: {}", indexedStorePath );
            contentIndex.remove( indexedStorePath );
            if ( pathConsumer != null )
            {
                pathConsumer.accept( indexedStorePath );
            }
        } );

        return paths;
    }

    public void removeIndexedStorePath( String path, StoreKey key, Consumer<IndexedStorePath> pathConsumer )
    {
//        Logger logger = LoggerFactory.getLogger( getClass() );
        IndexedStorePath topPath = new IndexedStorePath( key, path );
        if ( contentIndex.remove( topPath ) != null && pathConsumer != null )
        {
            pathConsumer.accept( topPath );
        }
//
//        QueryFactory queryFactory = Search.getQueryFactory( contentIndex );
//
//        QueryBuilder<Query> queryBuilder = queryFactory.from( IndexedStorePath.class )
//                                                       .having( "storeType" )
//                                                       .eq( key.getType() )
//                                                       .and()
//                                                       .having( "storeName" )
//                                                       .eq( key.getName() )
//                                                       .and()
//                                                       .having( "path" )
//                                                       .eq( path )
//                                                       .toBuilder();
//
//        List<IndexedStorePath> paths = queryBuilder.build().list();
//        paths.forEach( ( indexedStorePath ) -> {
//            logger.debug( "Removing: {}", indexedStorePath );
//            contentIndex.remove( indexedStorePath );
//            if ( pathConsumer != null )
//            {
//                pathConsumer.accept( indexedStorePath );
//            }
//        } );
    }

//    public List<IndexedStorePath> lookupIndexedPathByTopKey( final StoreKey key, final String path )
//    {
//        QueryFactory queryFactory = Search.getQueryFactory( contentIndex );
//        QueryBuilder<Query> queryBuilder = queryFactory.from( IndexedStorePath.class )
//                                                       .having( "storeType" )
//                                                       .eq( key.getType() )
//                                                       .and()
//                                                       .having( "storeName" )
//                                                       .eq( key.getName() )
//                                                       .and()
//                                                       .having( "path" )
//                                                       .eq( path )
//                                                       .toBuilder();
//
//        return queryBuilder.build().list();
//    }

//    public List<IndexedStorePath> lookupIndexedSubPathsByTopKey( final StoreKey key, final String superPath )
//    {
//        QueryFactory queryFactory = Search.getQueryFactory( contentIndex );
//        QueryBuilder<Query> queryBuilder = queryFactory.from( IndexedStorePath.class )
//                                                       .having( "storeType" )
//                                                       .eq( key.getType() )
//                                                       .and()
//                                                       .having( "storeName" )
//                                                       .eq( key.getName() )
//                                                       .and()
//                                                       .having( "path" )
//                                                       .like( superPath + "%" )
//                                                       .toBuilder();
//
//        return queryBuilder.build().list();
//    }

    public void deIndexStorePath( final StoreKey key, final String path )
    {
        executor.execute( () -> {
            IndexedStorePath toRemove = new IndexedStorePath( key, path );
            contentIndex.remove( toRemove );

            // TODO: Can we really make this lazy?
//            try
//            {
//                Set<Group> groups = storeDataManager.getGroupsContaining( key );
//                if ( groups != null )
//                {
//                    groups.forEach( ( group ) -> {
//                        IndexedStorePath groupToRemove = new IndexedStorePath( group.getKey(), path );
//                        contentIndex.remove( groupToRemove );
//                        byTopKey.remove( groupToRemove );
//                    } );
//                }
//            }
//            catch ( IndyDataException e )
//            {
//                Logger logger = LoggerFactory.getLogger( getClass() );
//                logger.error(
//                        String.format( "Cannot lookup groups containing: %s for content indexing. Reason: %s", key,
//                                       e.getMessage() ), e );
//            }
//
//            if ( StoreType.group == key.getType() )
//            {
//                try
//                {
//                    List<ArtifactStore> members = storeDataManager.getOrderedConcreteStoresInGroup( key.getName() );
//                    if ( members != null )
//                    {
//                        members.forEach( ( member ) -> {
//                            contentIndex.remove( new IndexedStorePath( key, member.getKey(), path ) );
//
//                            contentIndex.remove( new IndexedStorePath( member.getKey(), member.getKey(), path ) );
//                        } );
//                    }
//                }
//                catch ( IndyDataException e )
//                {
//                    Logger logger = LoggerFactory.getLogger( getClass() );
//                    logger.error( String.format(
//                            "Cannot lookup concrete membership of group: %s for content indexing. Reason: %s", key,
//                            e.getMessage() ), e );
//                }
//            }
        } );
    }

    public IndexedStorePath getIndexedStorePath( final StoreKey key, final String path )
            throws IndyWorkflowException
    {
        return contentIndex.get( new IndexedStorePath( key, path ) );

//        List<IndexedStorePath> matches = lookupIndexedPathByTopKey( key, path );
//        Logger logger = LoggerFactory.getLogger( getClass() );
//        logger.debug( "Found index hits for: {} in store: {}:\n  {}", path, key, matches );
//
//        if ( !matches.isEmpty() )
//        {
//            return matches.get( 0 );
//        }
//
//        return null;
    }

    public void indexTransferIn( Transfer transfer, StoreKey...topKeys )
    {
        if ( transfer != null && transfer.exists() )
        {
            indexPathInStores( transfer.getPath(), LocationUtils.getKey( transfer ), topKeys );
        }
    }

    /**
     * When we store or retrieve content, index it for faster reference next time.
     */
    public void indexPathInStores( String path, StoreKey originKey, StoreKey... topKeys )
    {
        executor.execute( () -> {
            IndexedStorePath origin = new IndexedStorePath( originKey, path );
            contentIndex.put( origin, origin );

            Set<StoreKey> keySet = new HashSet<>( Arrays.asList( topKeys ) );
            keySet.forEach( (key)->{
                IndexedStorePath isp = new IndexedStorePath( key, originKey, path );
                contentIndex.put( isp, origin );
            } );
        } );
    }

    public void clearIndexedPathFrom( String path, Set<Group> groups, Consumer<IndexedStorePath> pathConsumer )
    {
        if ( groups == null || groups.isEmpty() )
        {
            return;
        }

        Set<Group> nextGroups = new HashSet<>();
        groups.forEach( (group)->{
            removeIndexedStorePath( path, group.getKey(), pathConsumer );
            try
            {
                nextGroups.addAll( storeDataManager.getGroupsContaining( group.getKey() ) );
            }
            catch ( IndyDataException e )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.error( String.format( "Failed to lookup groups containing: %s. Reason: %s", group.getKey(), e.getMessage() ),
                              e );
            }
        } );

        nextGroups.removeAll( groups );

        clearIndexedPathFrom( path, nextGroups, pathConsumer );
    }

}
