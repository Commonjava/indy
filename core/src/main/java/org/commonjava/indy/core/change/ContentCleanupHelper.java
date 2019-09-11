package org.commonjava.indy.core.change;

import org.commonjava.cdi.util.weft.DrainingExecutorCompletionService;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DirectContentAccess;
import org.commonjava.indy.content.StoreContentAction;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import static org.commonjava.indy.core.change.StoreChangeUtil.delete;
import static org.commonjava.indy.core.change.StoreChangeUtil.listPathsAnd;

@ApplicationScoped
public class ContentCleanupHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DirectContentAccess directContentAccess;

    @Inject
    private Instance<StoreContentAction> storeContentActions;

    @Inject
    private SpecialPathManager specialPathManager;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    @WeftManaged
    @ExecutorConfig( threads=20, priority=7, named="content-cleanup" )
    private WeftExecutorService cleanupExecutor;

    public int clearPath( String path, ArtifactStore store )
    {
        logger.info( "Clear path: {}, store: {}", path, store.getKey() );
        try
        {
            delete( directContentAccess.getTransfer( store, path ) );
        }
        catch ( IndyWorkflowException e )
        {
            logger.warn( "Failed to delete path: {}, store: {}", path, store.getKey(), e );
        }

        boolean deleteOriginPath = true;
        StreamSupport.stream( storeContentActions.spliterator(), false )
                     .forEach( action -> action.clearStoreContent( path, store, Collections.emptySet(), deleteOriginPath ) );
        return 1;
    }

    public int clearPath( String path, ArtifactStore origin, Set<Group> affectedGroups, boolean deleteOriginPath )
    {
        logger.info( "Clear path: {}, origin: {}, affectedGroups: {}", path, origin.getKey(), affectedGroups );

        AtomicInteger cleared = new AtomicInteger( 0 );
        if ( deleteOriginPath && !storeDataManager.isReadonly( origin ) )
        {
            try
            {
                if ( delete( directContentAccess.getTransfer( origin, path ) ) )
                {
                    cleared.incrementAndGet();
                }
            }
            catch ( IndyWorkflowException e )
            {
                logger.warn( "Failed to delete path: {}, store: {}", path, origin.getKey(), e );
            }
        }

        affectedGroups.forEach( g -> {
            try
            {
                Transfer gt = directContentAccess.getTransfer( g, path );
                if ( delete( gt ) )
                {
                    cleared.incrementAndGet();
                }
            }
            catch ( IndyWorkflowException e )
            {
                logger.error( "Failed to retrieve transfer for: {} in group: {}", path, g.getName(), e );
            }
        } );

        logger.debug( "Clearing via store-content actions..." );
        StreamSupport.stream( storeContentActions.spliterator(), false )
                     .forEach( action -> action.clearStoreContent( path, origin, affectedGroups, deleteOriginPath ) );

        logger.debug( "Clear path done" );
        return cleared.get();
    }

    public void clearPaths( Set<StoreKey> keys, Predicate<? super String> pathFilter, boolean deleteOriginPath )
    {
        clearPaths( keys, pathFilter, null, deleteOriginPath );
    }

    /**
     * List the paths in target store and clean up the paths in affected groups.
     *
     * If groups are given, use them (for group update since all members share same group hierarchy). Otherwise,
     * query the affected groups (for store deletion and dis/enable event).
     */
    public void clearPaths( final Set<StoreKey> keys, Predicate<? super String> pathFilter, final Set<Group> groups,
                             final boolean deleteOriginPath )
    {
        //NOSSUP-76 we still need to use synchronized/drain way to clean the paths now, because sometimes the new used metadata
        //          not updated in time when some builds want to consume them as the obsolete metadata not cleared under
        //          async way.
        DrainingExecutorCompletionService<Integer> clearService =
                new DrainingExecutorCompletionService<>( cleanupExecutor );

        keys.forEach( key -> {
            ArtifactStore origin;
            try
            {
                origin = storeDataManager.getArtifactStore( key );
            }
            catch ( IndyDataException e )
            {
                logger.error( "Failed to retrieve store: " + key, e );
                return;
            }

            Set<Group> affected = groups;
            if ( affected == null )
            {
                try
                {
                    affected = ( storeDataManager.query().packageType( key.getPackageType() ).getGroupsAffectedBy( key ) );
                }
                catch ( IndyDataException e )
                {
                    logger.error( "Failed to retrieve groups affected by: " + key, e );
                    return;
                }
            }

            logger.debug( "Submit clean job for origin: {}", origin );
            final Set<Group> affectedGroups = affected;
            clearService.submit( clearPathsProcessor( origin, pathFilter, affectedGroups, deleteOriginPath ) );
        } );

        drainAndCount( clearService, "stores: " + keys );
    }

    public int drainAndCount( final DrainingExecutorCompletionService<Integer> clearService, final String description )
    {
        AtomicInteger count = new AtomicInteger( 0 );
        try
        {
            clearService.drain( count::addAndGet );
        }
        catch ( InterruptedException | ExecutionException e )
        {
            logger.error( "Failed to clear paths related to change in " + description, e );
        }

        logger.debug( "Cleared {} paths for changes in {}", count.get(), description );

        return count.get();
    }

    /**
     * We do clean-up in different ways. If the origin is hosted repo, we list it and clean the paths in affected groups.
     * If the origin is a remote repo, we find the affected groups, list them and clear ALL mergable paths. If the
     * origin is a group, we list it (cached files) and clean the paths from affected groups.
     */
    private Callable<Integer> clearPathsProcessor( ArtifactStore origin, Predicate<? super String> pathFilter,
                                                   Set<Group> affectedGroups, boolean deleteOriginPath )
    {
        if ( origin.getType() == StoreType.remote )
        {
            return () -> listPathsAnd( affectedGroups, mergablePath(), this::clearPath,
                                       this.directContentAccess );
        }
        else
        {
            return () -> listPathsAnd( origin.getKey(), pathFilter,
                                       p -> clearPath( p, origin, affectedGroups, deleteOriginPath ),
                                       this.directContentAccess );
        }
    }

    public Predicate<? super String> mergablePath()
    {
        return ( path ) -> {
            SpecialPathInfo pathInfo = specialPathManager.getSpecialPathInfo( path );
            return ( pathInfo != null && pathInfo.isMergable() );
        };
    }

    /**
     * Clean all paths including http-metadata, checksum, etc, for complete data integrity.
     */
    public Predicate<? super String> allPath()
    {
        return ( path ) -> true;
    }

}
