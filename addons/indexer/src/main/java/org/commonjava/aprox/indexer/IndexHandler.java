/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.indexer;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.lucene.store.LockReleaseFailedException;
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
import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.indexer.inject.IndexCreatorSet;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.shelflife.ExpirationManager;
import org.commonjava.shelflife.ExpirationManagerException;
import org.commonjava.shelflife.event.ExpirationEvent;
import org.commonjava.shelflife.model.Expiration;
import org.commonjava.shelflife.model.ExpirationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexHandler
{

    public static final long GROUP_INDEX_TIMEOUT = TimeUnit.MILLISECONDS.convert( 24, TimeUnit.HOURS );

    public static final long DEPLOY_POINT_INDEX_TIMEOUT = TimeUnit.MILLISECONDS.convert( 10, TimeUnit.MINUTES );

    public static final String INDEX_KEY_PREFIX = "aprox-index";

    private static final String INDEX_DIR = "/.index";

    private static final String INDEX_PROPERTIES = ".index/nexus-maven-repository-index-updater.properties";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

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
    @ExecutorConfig( daemon = true, priority = 7, named = "aprox-indexer" )
    private Executor executor;

    private final Set<StoreKey> currentlyUpdating = new HashSet<StoreKey>();

    public IndexHandler()
    {
    }

    public IndexHandler( final ExpirationManager expirationManager, final StoreDataManager storeDataManager, final FileManager fileManager )
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
        final Transfer item = event.getTransfer();
        final StoreKey key = LocationUtils.getKey( item );
        final String path = item.getPath();
        if ( !isIndexable( path ) )
        {
            return;
        }

        if ( key.getType() == StoreType.hosted )
        {
            HostedRepository store = null;
            try
            {
                store = storeDataManager.getHostedRepository( key.getName() );
            }
            catch ( final ProxyDataException e )
            {
                logger.error( String.format( "Failed to retrieve deploy-point for index update: %s. Reason: %s", key, e.getMessage() ), e );
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
                    logger.error( String.format( "Failed to schedule index update for deploy-point: %s. Reason: %s", key, e.getMessage() ), e );
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
        if ( path.endsWith( ".sha1" ) || path.endsWith( ".md5" ) || path.endsWith( "maven-metadata.xml" ) || path.endsWith( "archetype-catalog.xml" ) )
        {
            return false;
        }

        return true;
    }

    private boolean lock( final StoreKey key )
    {
        synchronized ( currentlyUpdating )
        {
            if ( currentlyUpdating.contains( key ) )
            {
                logger.info( "Already updating: {}", key );
                return false;
            }

            logger.info( "Reserving: {}", key );
            currentlyUpdating.add( key );
        }
        return true;
    }

    private void unlock( final StoreKey key )
    {
        synchronized ( currentlyUpdating )
        {
            logger.info( "Releasing: {}", key );
            currentlyUpdating.remove( key );
            currentlyUpdating.notifyAll();
        }
    }

    private void scanIndex( final ArtifactStore store )
    {
        final StoreKey key = store.getKey();
        if ( !lock( key ) )
        {
            return;
        }

        try
        {
            final IndexingContext context = getIndexingContext( store, indexCreators.getCreators() );

            if ( context == null )
            {
                return;
            }

            scanLockedIndex( store, context );
        }
        finally
        {
            unlock( key );
        }
    }

    private void scanLockedIndex( final ArtifactStore store, final IndexingContext context )
    {
        try
        {
            final ArtifactScanningListener listener = new DefaultScannerListener( context, indexerEngine, false, null );
            final ScanningRequest request = new ScanningRequest( context, listener );
            final ScanningResult result = scanner.scan( request );

            final List<Exception> exceptions = result.getExceptions();
            if ( exceptions != null && !exceptions.isEmpty() )
            {
                logger.error( "{}. While scanning: {}, encountered errors:\n\n  {}", store.getKey(), new JoinString( "\n\n  ", exceptions ) );
            }
            else
            {
                context.commit();
            }
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to commit changes to: %s. Reason: %s", store.getKey(), e.getMessage() ), e );
        }
        finally
        {
            try
            {
                context.close( false );
            }
            catch ( final IOException e )
            {
                logger.error( String.format( "Failed to close index for: %s. Reason: %s", store.getKey(), e.getMessage() ), e );
            }
        }
    }

    private void updateGroupsFor( final StoreKey storeKey, final Set<ArtifactStore> updated, final boolean updateRepositoryIndexes )
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

                    logger.info( "[CASCADE] Starting merge for: {}", group.getKey() );
                    updateMergedIndex( group, updated, updateRepositoryIndexes );
                }
            }
        }
        catch ( final ProxyDataException e )
        {
            logger.error( String.format( "Failed to retrieve groups that contain: %s. Reason: %s", storeKey, e.getMessage() ), e );
        }
    }

    private void updateMergedIndex( final Group group, final Set<ArtifactStore> updated, final boolean updateRepositoryIndexes )
    {
        final StoreKey groupKey = group.getKey();
        if ( !lock( groupKey ) )
        {
            return;
        }

        try
        {

            final IndexingContext groupContext = getIndexingContext( group, indexCreators.getCreators() );
            if ( groupContext == null )
            {
                return;
            }

            try
            {
                final List<ArtifactStore> stores = storeDataManager.getOrderedConcreteStoresInGroup( group.getName() );
                for ( final ArtifactStore store : stores )
                {
                    if ( updated.contains( store ) )
                    {
                        continue;
                    }

                    final StoreKey key = store.getKey();
                    if ( !lock( key ) )
                    {
                        continue;
                    }

                    IndexingContext context = getIndexingContext( store, indexCreators.getCreators() );
                    try
                    {
                        if ( context == null )
                        {
                            continue;
                        }

                        final Transfer item = fileManager.getStorageReference( store, INDEX_PROPERTIES );
                        if ( !item.exists() )
                        {
                            if ( updateRepositoryIndexes || key.getType() == StoreType.hosted )
                            {
                                scanLockedIndex( store, context );
                            }
                        }
                        else if ( updateRepositoryIndexes && key.getType() == StoreType.remote )
                        {
                            doIndexUpdate( context, key );
                        }

                        updated.add( store );

                        context = getIndexingContext( store, indexCreators.getCreators() );

                        if ( context == null )
                        {
                            continue;
                        }

                        try
                        {
                            if ( context.getIndexDirectory() != null && context.getIndexDirectoryFile()
                                                                               .exists() )
                            {
                                groupContext.merge( context.getIndexDirectory() );
                            }

                            groupContext.commit();
                        }
                        catch ( final IOException e )
                        {
                            logger.error( String.format( "Failed to merge index from: %s into group index: %s. Reason: %s", key, group.getKey(),
                                                         e.getMessage() ), e );
                        }
                    }
                    finally
                    {
                        if ( context != null )
                        {
                            try
                            {
                                context.commit();
                                context.close( false );
                            }
                            catch ( final IOException e )
                            {
                                logger.error( String.format( "Failed to close context for: %s. Reason: %s", key, e.getMessage() ), e );
                            }
                        }

                        unlock( key );
                    }
                }

                try
                {
                    groupContext.commit();
                }
                catch ( final IOException e )
                {
                    logger.error( String.format( "Failed to commit index updates for group: %s. Reason: %s", group.getKey(), e.getMessage() ), e );
                }

                updated.add( group );

                try
                {
                    final Expiration exp = expirationForGroup( group.getName() );

                    expirationManager.schedule( exp );

                    logger.info( "Next index update in group: {} scheduled for: {}", group.getName(), new Date( exp.getExpires() ) );
                }
                catch ( final ExpirationManagerException e )
                {
                    logger.error( String.format( "Failed to schedule indexer trigger for group: %s. Reason: %s", group.getName(), e.getMessage() ), e );
                }
            }
            catch ( final ProxyDataException e )
            {
                logger.error( String.format( "Failed to retrieve concrete stores in group: %s. Reason: %s", groupKey, e.getMessage() ), e );
                return;
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
                        logger.error( String.format( "Failed to close indexing context: %s", e.getMessage() ), e );
                    }
                }
            }
        }
        finally
        {
            unlock( groupKey );
        }

        logger.info( "Index updated for: {}", groupKey );
    }

    private IndexUpdateResult doIndexUpdate( final IndexingContext mergedContext, final StoreKey key )
    {
        try
        {
            final ResourceFetcher resourceFetcher = new AproxResourceFetcher( storeDataManager, fileManager );

            final Date centralContextCurrentTimestamp = mergedContext.getTimestamp();
            final IndexUpdateRequest updateRequest = new IndexUpdateRequest( mergedContext, resourceFetcher );
            IndexUpdateResult updateResult = null;
            try
            {
                updateResult = indexUpdater.fetchAndUpdateIndex( updateRequest );
                mergedContext.commit();
            }
            catch ( final IOException e )
            {
                logger.error( String.format( "Failed to update index for: %s. Reason: %s", key, e.getMessage() ), e );
            }

            if ( updateResult == null )
            {
                return null;
            }

            if ( updateResult.isFullUpdate() )
            {
                logger.info( "FULL index update completed for: {}", key );
            }
            else if ( updateResult.getTimestamp() != null && updateResult.getTimestamp()
                                                                         .equals( centralContextCurrentTimestamp ) )
            {
                logger.info( "NO index update for: {}. Index is up-to-date.", key );
            }
            else
            {
                logger.info( "INCREMENTAL index update completed for: {} to cover period: {} - {}", key, centralContextCurrentTimestamp,
                             updateResult.getTimestamp() );
            }

            return updateResult;
        }
        finally
        {
            if ( mergedContext != null )
            {
                try
                {
                    mergedContext.close( false );
                }
                catch ( final IOException e )
                {
                    logger.error( String.format( "Failed to close index for: %s. Reason: %s", key, e.getMessage() ), e );
                }
            }
        }
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
            /* TODO:
            15:19:27,359 ERROR [org.commonjava.aprox.indexer.IndexHandler] (aprox-indexer-0) 
            Failed to create indexing context for: repository:central. 
                Reason: Cannot forcefully unlock a NativeFSLock which is held by 
                another indexer component: /var/lib/aprox/storage/repository-central/.index/write.lock: 
            org.apache.lucene.store.LockReleaseFailedException: 
                Cannot forcefully unlock a NativeFSLock which is held by another 
                indexer component: /var/lib/aprox/storage/repository-central/.index/write.lock
            at org.apache.lucene.store.NativeFSLock.release(NativeFSLockFactory.java:295) [lucene-core-3.6.1.jar:3.6.1 1362471 - thetaphi - 2012-07-17 12:40:12]
            at org.apache.lucene.index.IndexWriter.unlock(IndexWriter.java:4624) [lucene-core-3.6.1.jar:3.6.1 1362471 - thetaphi - 2012-07-17 12:40:12]
            at org.apache.maven.index.context.DefaultIndexingContext.prepareCleanIndex(DefaultIndexingContext.java:232) [indexer-core-5.1.0.jar:5.1.0]
            at org.apache.maven.index.context.DefaultIndexingContext.prepareIndex(DefaultIndexingContext.java:206) [indexer-core-5.1.0.jar:5.1.0]
            at org.apache.maven.index.context.DefaultIndexingContext.<init>(DefaultIndexingContext.java:147) [indexer-core-5.1.0.jar:5.1.0]
            at org.apache.maven.index.context.DefaultIndexingContext.<init>(DefaultIndexingContext.java:155) [indexer-core-5.1.0.jar:5.1.0]
            at org.apache.maven.index.DefaultIndexer.createIndexingContext(DefaultIndexer.java:76) [indexer-core-5.1.0.jar:5.1.0]
            at org.commonjava.aprox.indexer.IndexHandler.getIndexingContext(IndexHandler.java:442) [classes:]
            at org.commonjava.aprox.indexer.IndexHandler.getContextsFor(IndexHandler.java:411) [classes:]
            at org.commonjava.aprox.indexer.IndexHandler.updateMergedIndex(IndexHandler.java:264) [classes:]
            at org.commonjava.aprox.indexer.IndexHandler.access$300(IndexHandler.java:57) [classes:]
            at org.commonjava.aprox.indexer.IndexHandler$AdditionRunnable.run(IndexHandler.java:578) [classes:]
            at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145) [rt.jar:1.7.0_25]
            at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615) [rt.jar:1.7.0_25]
            at java.lang.Thread.run(Thread.java:724) [rt.jar:1.7.0_25]
             */
            final IndexingContext context = indexer.createIndexingContext( id, id, rootDir, indexDir, id, null, true, true, indexers );

            return context;
        }
        catch ( final LockReleaseFailedException e )
        {
            //            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(), e.getMessage() ), e );
            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(), e.getMessage() ) );
        }
        catch ( final ExistingLuceneIndexMismatchException e )
        {
            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(), e.getMessage() ), e );
        }
        catch ( final IllegalArgumentException e )
        {
            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(), e.getMessage() ), e );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(), e.getMessage() ), e );
        }

        return null;
    }

    private Expiration expirationForGroup( final String name )
    {
        return new Expiration( new ExpirationKey( StoreType.group.name(), INDEX_KEY_PREFIX, name ), GROUP_INDEX_TIMEOUT,
                               new StoreKey( StoreType.group, name ) );
    }

    private Expiration expirationForDeployPoint( final String name )
    {
        return new Expiration( new ExpirationKey( StoreType.hosted.name(), INDEX_KEY_PREFIX, name ), DEPLOY_POINT_INDEX_TIMEOUT,
                               new StoreKey( StoreType.hosted, name ) );
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
                logger.error( String.format( "Failed to update index for: %s. Reason: %s", key, e.getMessage() ), e );
                return;
            }

            if ( store == null )
            {
                logger.error( "Failed to update index for: {}. Reason: Cannot find corresponding ArtifactStore", key );
                return;
            }

            if ( type == StoreType.hosted )
            {
                scanIndex( store );
            }
            else if ( type == StoreType.group )
            {
                logger.info( "[IDX] Starting merge for: {}", store.getKey() );
                updateMergedIndex( (Group) store, new HashSet<ArtifactStore>(), false );
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
                    updateGroupsFor( new StoreKey( type, name ), updated, true );
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
                        logger.error( String.format( "Failed to cancel indexer trigger for group: %s. Reason: %s", name, e.getMessage() ), e );
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

                    logger.info( "[ADD] Starting merge for: {}", group.getKey() );
                    updateMergedIndex( group, updated, true );
                }
                else
                {
                    if ( store.getKey()
                              .getType() == StoreType.hosted )
                    {
                        scanIndex( store );
                    }

                    updateGroupsFor( store.getKey(), updated, true );
                }
            }
        }
    }

}
