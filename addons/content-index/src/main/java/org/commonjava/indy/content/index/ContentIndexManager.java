package org.commonjava.indy.content.index;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
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
    @Inject
    private Cache<IndexedStorePath, IndexedStorePath> contentIndex;

    @ExecutorConfig( named = "content-indexer", threads = 8, priority = 2, daemon = true )
    @WeftManaged
    @Inject
    private Executor executor;


    public void removeAllOriginIndexedPathsForStore( StoreKey memberKey )
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

        queryBuilder.build().list().forEach( ( idx ) -> {
            logger.debug( "Removing {}", idx );
            contentIndex.remove( idx );
        } );
    }

    public void removeAllIndexedPathsForStore( StoreKey key )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        // invalidate indexes for the store itself
        QueryFactory queryFactory = Search.getQueryFactory( contentIndex );
        QueryBuilder<Query> queryBuilder = queryFactory.from( IndexedStorePath.class )
                                                       .having( "storeType" )
                                                       .eq( key.getType() )
                                                       .and()
                                                       .having( "storeName" )
                                                       .eq( key.getName() )
                                                       .toBuilder();

        queryBuilder.build().list().forEach( ( idx ) -> {
            logger.debug( "Removing: {}", idx );
            contentIndex.remove( idx );
        } );
    }

    public void removeOriginIndexedStorePath( String path, StoreKey key )
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

        queryBuilder.build().list().forEach( ( idx ) -> {
            logger.debug( "Removing: {}", idx );
            contentIndex.remove( idx );
        } );
    }

    public void removeIndexedStorePath( String path, StoreKey key )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

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

        queryBuilder.build().list().forEach( ( idx ) -> {
            logger.debug( "Removing: {}", idx );
            contentIndex.remove( idx );
        } );
    }

    public List<IndexedStorePath> lookupIndexedPathByTopKey( final StoreKey key, final String path )
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

    public List<IndexedStorePath> lookupIndexedSubPathsByTopKey( final StoreKey key, final String superPath )
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
                                                       .like( superPath + "%" )
                                                       .toBuilder();

        return queryBuilder.build().list();
    }

    public void deIndexStorePath( final StoreKey key, final String path )
    {
        executor.execute( () -> {
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

    public IndexedStorePath getIndexedStorePath( final StoreKey key, final String path )
            throws IndyWorkflowException
    {
        List<IndexedStorePath> matches = lookupIndexedPathByTopKey( key, path );
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Found index hits for: {} in store: {}:\n  {}", path, key, matches );

        if ( !matches.isEmpty() )
        {
            return findFirstMatchingIndexedStorePath( matches, key );
        }

        return null;
    }

    public void indexTransferIn( Transfer transfer, StoreKey...keys )
    {
        if ( transfer != null && transfer.exists() )
        {
            indexPathInStores( transfer.getPath(), keys );
        }
    }

    /**
     * When we store or retrieve content, index it for faster reference next time.
     */
    public void indexPathInStores( String path, StoreKey... keys )
    {
        executor.execute( () -> {
            Set<StoreKey> keySet = new HashSet<>( Arrays.asList( keys ) );
            keySet.forEach( (key)->{
                IndexedStorePath isp = new IndexedStorePath( key, key, path );
                contentIndex.put( isp, isp );

                indexPathInGroupsContainingStore( key, key, path );
            } );
        } );
    }

    /**
     * When we index content, also index it for groups containing its origin stores, to make indexes more efficient.
     * This will be called recursively for groups of groups.
     */
    private void indexPathInGroupsContainingStore( final StoreKey key, final StoreKey originKey, final String path )
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
    private IndexedStorePath findFirstMatchingIndexedStorePath( final List<IndexedStorePath> matches, final StoreKey key )
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
