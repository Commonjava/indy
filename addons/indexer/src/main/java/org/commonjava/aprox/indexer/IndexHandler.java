package org.commonjava.aprox.indexer;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.maven.index.ArtifactScanningListener;
import org.apache.maven.index.DefaultScannerListener;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IndexerEngine;
import org.apache.maven.index.Scanner;
import org.apache.maven.index.ScanningRequest;
import org.apache.maven.index.ScanningResult;
import org.apache.maven.index.context.ExistingLuceneIndexMismatchException;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.change.event.FileStorageEvent;
import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.indexer.inject.IndexCreatorSet;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.shelflife.ExpirationManager;
import org.commonjava.shelflife.ExpirationManagerException;
import org.commonjava.shelflife.event.ExpirationEvent;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class IndexHandler
{

    public static final long GROUP_INDEX_TIMEOUT = TimeUnit.MILLISECONDS.convert( 24, TimeUnit.HOURS );

    public static final long DEPLOY_POINT_INDEX_TIMEOUT = TimeUnit.MILLISECONDS.convert( 10, TimeUnit.MINUTES );

    public static final String INDEX_KEY_PREFIX = "aprox-index";

    private static final String INDEX_DIR = "/.index";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private Indexer indexer;

    @Inject
    private IndexerEngine indexerEngine;

    @Inject
    private Scanner scanner;

    @Inject
    private IndexUpdater indexUpdater;

    @Inject
    private IndexCreatorSet indexCreators;

    @Inject
    private ExpirationManager expirationManager;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private FileManager fileManager;

    @Inject
    @ExecutorConfig( daemon = true, priority = 7, threads = 1, named = "aprox-indexer" )
    private Executor executor;

    public IndexHandler()
    {
    }

    public IndexHandler( final ExpirationManager expirationManager, final StoreDataManager storeDataManager,
                         final FileManager fileManager )
        throws AproxIndexerException
    {
        this.expirationManager = expirationManager;
        this.storeDataManager = storeDataManager;
        this.fileManager = fileManager;
    }

    public void onDelete( @Observes final ProxyManagerDeleteEvent event )
    {
        executor.execute( new DeletionRunnable( event ) );
    }

    public void onStorage( @Observes final FileStorageEvent event )
    {
        final StorageItem item = event.getStorageItem();
        final StoreKey key = item.getStoreKey();
        final String path = item.getPath();
        if ( !isIndexable( path ) )
        {
            return;
        }

        if ( key.getType() == StoreType.deploy_point )
        {
            DeployPoint store = null;
            try
            {
                store = storeDataManager.getDeployPoint( key.getName() );
            }
            catch ( final ProxyDataException e )
            {
                logger.error( "Failed to retrieve deploy-point for index update: %s. Reason: %s", e, key,
                              e.getMessage() );
            }

            if ( store != null )
            {
                final Expiration exp = expirationForDeployPoint( key.getName() );
                try
                {
                    if ( !expirationManager.contains( exp ) )
                    {
                        expirationManager.schedule( exp );
                    }
                }
                catch ( final ExpirationManagerException e )
                {
                    logger.error( "Failed to schedule index update for deploy-point: %s. Reason: %s", e, key,
                                  e.getMessage() );
                }
            }
        }
    }

    public void onExpire( @Observes final ExpirationEvent event )
    {
        final Expiration expiration = event.getExpiration();
        final String[] parts = expiration.getKey()
                                         .getParts();
        if ( !INDEX_KEY_PREFIX.equals( parts[0] ) )
        {
            return;
        }

        executor.execute( new IndexExpirationRunnable( expiration ) );
    }

    public void onAdd( @Observes final ArtifactStoreUpdateEvent event )
    {
        executor.execute( new AdditionRunnable( event ) );
    }

    private boolean isIndexable( final String path )
    {
        if ( path.endsWith( ".sha1" ) || path.endsWith( ".md5" ) || path.endsWith( "maven-metadata.xml" )
            || path.endsWith( "archetype-catalog.xml" ) )
        {
            return false;
        }

        return true;
    }

    private synchronized void updateDeployPointIndex( final DeployPoint store )
    {
        final IndexingContext context = getIndexingContext( store, indexCreators.getCreators() );

        if ( context == null )
        {
            return;
        }

        try
        {
            final ArtifactScanningListener listener = new DefaultScannerListener( context, indexerEngine, false, null );
            final ScanningRequest request = new ScanningRequest( context, listener );
            final ScanningResult result = scanner.scan( request );

            final List<Exception> exceptions = result.getExceptions();
            if ( exceptions != null && !exceptions.isEmpty() )
            {
                logger.error( "%d. While scanning: %s, encountered errors:\n\n  %s", store.getKey(),
                              join( exceptions, "\n\n  " ) );
            }
        }
        finally
        {
            try
            {
                context.close( false );
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to close index for deploy point: %s. Reason: %s", e, store.getKey(),
                              e.getMessage() );
            }
        }
    }

    private synchronized void updateGroupsFor( final StoreKey storeKey, final Set<ArtifactStore> updated )
    {
        try
        {
            final Set<Group> groups = storeDataManager.getGroupsContaining( storeKey );
            if ( groups != null )
            {
                for ( final Group group : groups )
                {
                    if ( updated.contains( group ) )
                    {
                        continue;
                    }

                    updateMergedIndex( group, updated );
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve groups that contain: %s. Reason: %s", e, storeKey, e.getMessage() );
        }
    }

    private synchronized void updateMergedIndex( final Group group, final Set<ArtifactStore> updated )
    {
        final IndexingContext groupContext = getIndexingContext( group, indexCreators.getCreators() );
        if ( groupContext == null )
        {
            return;
        }

        final Map<ArtifactStore, IndexingContext> contexts = getContextsFor( group, indexCreators.getCreators() );
        try
        {
            for ( final Map.Entry<ArtifactStore, IndexingContext> entry : contexts.entrySet() )
            {
                final ArtifactStore store = entry.getKey();
                if ( updated.contains( store ) )
                {
                    continue;
                }

                final StoreKey key = store.getKey();
                final IndexingContext context = entry.getValue();

                doIndexUpdate( context, key );
                updated.add( store );

                try
                {
                    groupContext.merge( context.getIndexDirectory() );
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to merge index from: %s into group index: %s", key, group.getKey() );
                }
            }

            try
            {
                groupContext.commit();
            }
            catch ( final IOException e )
            {
                logger.error( "Failed to commit index updates for group: %s. Reason: %s", e, group.getKey(),
                              e.getMessage() );
            }

            updated.add( group );

            try
            {
                final Expiration exp = expirationForGroup( group.getName() );

                expirationManager.schedule( exp );

                logger.info( "Next index update in group: %s scheduled for: %s", group.getName(),
                             new Date( exp.getExpires() ) );
            }
            catch ( final ExpirationManagerException e )
            {
                logger.error( "Failed to schedule indexer trigger for group: %s. Reason: %s", e, group.getName(),
                              e.getMessage() );
            }
        }
        finally
        {
            if ( groupContext != null )
            {
                try
                {
                    groupContext.close( false );
                }
                catch ( final IOException e )
                {
                    logger.error( "Failed to close indexing context: %s", e, e.getMessage() );
                }
            }

            if ( contexts != null )
            {
                for ( final IndexingContext ctx : contexts.values() )
                {
                    try
                    {
                        ctx.close( false );
                    }
                    catch ( final IOException e )
                    {
                        logger.error( "Failed to close indexing context: %s", e, e.getMessage() );
                    }
                }
            }
        }
    }

    private IndexUpdateResult doIndexUpdate( final IndexingContext mergedContext, final StoreKey key )
    {
        final ResourceFetcher resourceFetcher = new AproxResourceFetcher( storeDataManager, fileManager );

        final Date centralContextCurrentTimestamp = mergedContext.getTimestamp();
        final IndexUpdateRequest updateRequest = new IndexUpdateRequest( mergedContext, resourceFetcher );
        IndexUpdateResult updateResult = null;
        try
        {
            updateResult = indexUpdater.fetchAndUpdateIndex( updateRequest );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to update index for: %s. Reason: %s", key, e.getMessage() );
        }

        if ( updateResult == null )
        {
            return null;
        }

        if ( updateResult.isFullUpdate() )
        {
            logger.info( "FULL index update completed for: %s", key );
        }
        else if ( updateResult.getTimestamp()
                              .equals( centralContextCurrentTimestamp ) )
        {
            logger.info( "NO index update for: %s. Index is up-to-date.", key );
        }
        else
        {
            logger.info( "INCREMENTAL index update completed for: %s to cover period: %s - %s", key,
                         centralContextCurrentTimestamp, updateResult.getTimestamp() );
        }

        return updateResult;
    }

    private Map<ArtifactStore, IndexingContext> getContextsFor( final Group group, final List<IndexCreator> indexers )
    {
        Map<ArtifactStore, IndexingContext> contexts = null;
        try
        {
            final List<ArtifactStore> stores = storeDataManager.getOrderedConcreteStoresInGroup( group.getName() );
            if ( stores != null && !stores.isEmpty() )
            {
                contexts = new LinkedHashMap<ArtifactStore, IndexingContext>( stores.size() );
                for ( final ArtifactStore store : stores )
                {
                    final IndexingContext ctx = getIndexingContext( store, indexers );
                    if ( ctx != null )
                    {
                        contexts.put( store, ctx );
                    }
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to retrieve ordered concrete stores in group: %s. Reason: %s", e, group.getName(),
                          e.getMessage() );
        }

        return contexts;
    }

    private IndexingContext getIndexingContext( final ArtifactStore store, final List<IndexCreator> indexers )
    {
        final File indexDir = fileManager.getStorageReference( store, INDEX_DIR )
                                         .getDetachedFile();
        indexDir.mkdirs();

        final File rootDir = fileManager.getStorageReference( store, FileManager.ROOT_PATH )
                                        .getDetachedFile();

        final String id = store.getKey()
                               .toString();

        try
        {
            return indexer.createIndexingContext( id, id, rootDir, indexDir, id, null, true, true, indexers );
        }
        catch ( final ExistingLuceneIndexMismatchException e )
        {
            logger.error( "Failed to create indexing context for: %s. Reason: %s", e, store.getKey(), e.getMessage() );
        }
        catch ( final IllegalArgumentException e )
        {
            logger.error( "Failed to create indexing context for: %s. Reason: %s", e, store.getKey(), e.getMessage() );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to create indexing context for: %s. Reason: %s", e, store.getKey(), e.getMessage() );
        }

        return null;
    }

    private Expiration expirationForGroup( final String name )
    {
        return new Expiration( new ExpirationKey( StoreType.group.name(), INDEX_KEY_PREFIX, name ),
                               GROUP_INDEX_TIMEOUT, new StoreKey( StoreType.group, name ) );
    }

    private Expiration expirationForDeployPoint( final String name )
    {
        return new Expiration( new ExpirationKey( StoreType.deploy_point.name(), INDEX_KEY_PREFIX, name ),
                               DEPLOY_POINT_INDEX_TIMEOUT, new StoreKey( StoreType.deploy_point, name ) );
    }

    public class IndexExpirationRunnable
        implements Runnable
    {
        private final Expiration expiration;

        public IndexExpirationRunnable( final Expiration expiration )
        {
            this.expiration = expiration;
        }

        @Override
        public void run()
        {
            final StoreKey key = StoreKey.fromString( (String) expiration.getData() );
            final StoreType type = key.getType();

            ArtifactStore store;
            try
            {
                store = storeDataManager.getArtifactStore( key );
            }
            catch ( final ProxyDataException e )
            {
                logger.error( "Failed to update index for: %s. Reason: %s", e, key, e.getMessage() );
                return;
            }

            if ( type == StoreType.deploy_point )
            {
                updateDeployPointIndex( (DeployPoint) store );
            }
            else if ( type == StoreType.group )
            {
                updateMergedIndex( (Group) store, new HashSet<ArtifactStore>() );
            }
        }
    }

    public class DeletionRunnable
        implements Runnable
    {
        private final ProxyManagerDeleteEvent event;

        public DeletionRunnable( final ProxyManagerDeleteEvent event )
        {
            this.event = event;
        }

        @Override
        public void run()
        {
            final StoreType type = event.getType();
            if ( type != StoreType.group )
            {
                final Set<ArtifactStore> updated = new HashSet<ArtifactStore>();
                for ( final String name : event )
                {
                    updateGroupsFor( new StoreKey( type, name ), updated );
                }
            }
            else
            {
                for ( final String name : event )
                {
                    try
                    {
                        expirationManager.cancel( expirationForGroup( name ) );
                    }
                    catch ( final ExpirationManagerException e )
                    {
                        logger.error( "Failed to cancel indexer trigger for group: %s. Reason: %s", e, name,
                                      e.getMessage() );
                    }
                }
            }
        }
    }

    public class AdditionRunnable
        implements Runnable
    {
        private final ArtifactStoreUpdateEvent event;

        public AdditionRunnable( final ArtifactStoreUpdateEvent event )
        {
            this.event = event;
        }

        @Override
        public void run()
        {
            final Set<ArtifactStore> updated = new HashSet<ArtifactStore>();
            for ( final ArtifactStore store : event )
            {
                if ( store.getKey()
                          .getType() == StoreType.group )
                {
                    final Group group = (Group) store;
                    if ( updated.contains( group ) )
                    {
                        continue;
                    }

                    updateMergedIndex( group, updated );
                }
                else
                {
                    if ( store.getKey()
                              .getType() == StoreType.deploy_point )
                    {
                        updateDeployPointIndex( (DeployPoint) store );
                    }

                    updateGroupsFor( store.getKey(), updated );
                }
            }
        }
    }

}
