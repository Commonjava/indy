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
import org.commonjava.aprox.change.event.AbstractStoreDeleteEvent;
import org.commonjava.aprox.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.core.expire.AproxSchedulerException;
import org.commonjava.aprox.core.expire.ScheduleManager;
import org.commonjava.aprox.core.expire.SchedulerEvent;
import org.commonjava.aprox.core.expire.StoreKeyMatcher;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.indexer.conf.IndexerConfig;
import org.commonjava.aprox.indexer.inject.IndexCreatorSet;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.ident.util.JoinString;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexHandler
{

    public static final int GROUP_INDEX_TIMEOUT_SECONDS = (int) TimeUnit.SECONDS.convert( 24, TimeUnit.HOURS );

    public static final int HOSTED_REPO_INDEX_TIMEOUT_SECONDS = (int) TimeUnit.SECONDS.convert( 10, TimeUnit.MINUTES );

    public static final String INDEX_KEY_PREFIX = "aprox-index";

    private static final String INDEX_DIR = "/.index";

    private static final String INDEX_PROPERTIES = ".index/nexus-maven-repository-index-updater.properties";

    private static final String REINDEX_JOB_TYPE = "REINDEX";

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
    private ScheduleManager scheduleManager;

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private DownloadManager fileManager;

    @Inject
    private IndexerConfig config;

    @Inject
    @ExecutorConfig( daemon = true, priority = 7, named = "aprox-indexer" )
    private Executor executor;

    private final Set<StoreKey> currentlyUpdating = new HashSet<StoreKey>();

    public IndexHandler()
    {
    }

    public IndexHandler( final ScheduleManager scheduleManager, final StoreDataManager storeDataManager,
                         final DownloadManager fileManager, final IndexerConfig config )
        throws AproxIndexerException
    {
        this.scheduleManager = scheduleManager;
        this.storeDataManager = storeDataManager;
        this.fileManager = fileManager;
        this.config = config;
    }

    public void onDelete( @Observes final AbstractStoreDeleteEvent event )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        logger.info( "Updating indexes as a result of ProxyManagerDeleteEvent." );
        executor.execute( new DeletionRunnable( event ) );
    }

    public void onStorage( @Observes final FileStorageEvent event )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        logger.info( "Handling storage event: {}", event );
        final Transfer item = event.getTransfer();
        final StoreKey key = LocationUtils.getKey( item );
        final String path = item.getPath();
        if ( !isIndexable( path ) )
        {
            return;
        }

        int timeoutSeconds = -1;
        if ( key.getType() == StoreType.group )
        {
            timeoutSeconds = GROUP_INDEX_TIMEOUT_SECONDS;
        }
        else if ( key.getType() == StoreType.hosted )
        {
            timeoutSeconds = HOSTED_REPO_INDEX_TIMEOUT_SECONDS;
        }

        if ( timeoutSeconds > 0 )
        {
            if ( storeDataManager.hasArtifactStore( key ) )
            {
                try
                {
                    final TriggerKey tk =
                        scheduleManager.findFirstMatchingTrigger( new StoreKeyMatcher( key, REINDEX_JOB_TYPE ) );

                    if ( tk == null )
                    {

                        scheduleManager.scheduleForStore( key, REINDEX_JOB_TYPE, REINDEX_JOB_TYPE, key, timeoutSeconds,
                                                          timeoutSeconds );
                    }
                }
                catch ( final AproxSchedulerException e )
                {
                    logger.error( "Failed to schedule reindex for: " + key, e );
                }
            }
            else
            {
                logger.error( "No such ArtifactStore: {}", key );
            }
        }
    }

    public void onExpire( @Observes final SchedulerEvent event )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        if ( !event.getJobType()
                   .equals( REINDEX_JOB_TYPE ) )
        {
            return;
        }

        logger.info( "Updating indexes as a result of ExpirationEvent." );
        final StoreKey storeKey = StoreKey.fromString( event.getPayload() );
        if ( storeKey == null )
        {
            logger.warn( "No StoreKey found in payload of: {}.", event );
            return;
        }

        executor.execute( new IndexExpirationRunnable( storeKey ) );
    }

    public void onAdd( @Observes final ArtifactStorePostUpdateEvent event )
    {
        if ( !config.isEnabled() )
        {
            return;
        }

        logger.info( "Updating indexes as a result of ArtifactStoreUpdateEvent." );
        executor.execute( new AdditionRunnable( event ) );
    }

    private boolean isIndexable( final String path )
    {
        return !( path.endsWith( ".sha1" ) || path.endsWith( ".md5" ) || path.endsWith( "maven-metadata.xml" )
                        || path.endsWith( "archetype-catalog.xml" ) );

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
                logger.error( "{}. While scanning: {}, encountered errors:\n\n  {}", store.getKey(),
                              new JoinString( "\n\n  ", exceptions ) );
            }
            else
            {
                context.commit();
            }
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to commit changes to: %s. Reason: %s", store.getKey(), e.getMessage() ),
                          e );
        }
        finally
        {
            try
            {
                context.close( false );
            }
            catch ( final IOException e )
            {
                logger.error( String.format( "Failed to close index for: %s. Reason: %s", store.getKey(),
                                             e.getMessage() ), e );
            }
        }
    }

    private void updateGroupsFor( final StoreKey storeKey, final Set<ArtifactStore> updated,
                                  final boolean updateRepositoryIndexes )
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
                        logger.info( "Already updated group: {} (contains: {}). Skipping", group, storeKey );
                        continue;
                    }

                    logger.info( "[CASCADE] Starting merge for: {}", group.getKey() );
                    updateMergedIndex( group, updated, updateRepositoryIndexes );
                }
            }
        }
        catch ( final AproxDataException e )
        {
            logger.error( String.format( "Failed to retrieve groups that contain: %s. Reason: %s", storeKey,
                                         e.getMessage() ), e );
        }
    }

    private void updateMergedIndex( final Group group, final Set<ArtifactStore> updated,
                                    final boolean updateRepositoryIndexes )
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

            logger.info( "Marking as updated: {}", group );
            updated.add( group );

            try
            {
                final List<ArtifactStore> stores = storeDataManager.getOrderedConcreteStoresInGroup( group.getName() );
                for ( final ArtifactStore store : stores )
                {
                    if ( updated.contains( store ) )
                    {
                        logger.info( "Already updated: {}. Skipping", store );
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

                        logger.info( "Marking as updated: {}", store );
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
                            logger.error( String.format( "Failed to merge index from: %s into group index: %s. Reason: %s",
                                                         key, group.getKey(), e.getMessage() ), e );
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
                                logger.error( String.format( "Failed to close context for: %s. Reason: %s", key,
                                                             e.getMessage() ), e );
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
                    logger.error( String.format( "Failed to commit index updates for group: %s. Reason: %s",
                                                 group.getKey(), e.getMessage() ), e );
                }
            }
            catch ( final AproxDataException e )
            {
                logger.error( String.format( "Failed to retrieve concrete stores in group: %s. Reason: %s", groupKey,
                                             e.getMessage() ), e );
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
                logger.info( "INCREMENTAL index update completed for: {} to cover period: {} - {}", key,
                             centralContextCurrentTimestamp, updateResult.getTimestamp() );
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

        final File rootDir = fileManager.getStorageReference( store, DownloadManager.ROOT_PATH )
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
            final IndexingContext context =
                indexer.createIndexingContext( id, id, rootDir, indexDir, id, null, true, true, indexers );

            return context;
        }
        catch ( final LockReleaseFailedException e )
        {
            //            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(), e.getMessage() ), e );
            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(),
                                         e.getMessage() ) );
        }
        catch ( final ExistingLuceneIndexMismatchException e )
        {
            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(),
                                         e.getMessage() ), e );
        }
        catch ( final IllegalArgumentException e )
        {
            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(),
                                         e.getMessage() ), e );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "Failed to create indexing context for: %s. Reason: %s", store.getKey(),
                                         e.getMessage() ), e );
        }

        return null;
    }

    public class IndexExpirationRunnable
        implements Runnable
    {
        private final StoreKey key;

        public IndexExpirationRunnable( final StoreKey key )
        {
            this.key = key;
        }

        @Override
        public void run()
        {
            if ( key == null )
            {
                return;
            }

            final StoreType type = key.getType();

            ArtifactStore store;
            try
            {
                store = storeDataManager.getArtifactStore( key );
            }
            catch ( final AproxDataException e )
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
        private final AbstractStoreDeleteEvent event;

        public DeletionRunnable( final AbstractStoreDeleteEvent event )
        {
            this.event = event;
        }

        @Override
        public void run()
        {
            for ( final ArtifactStore store : event )
            {
                if ( StoreType.group != store.getKey()
                                             .getType() )
                {
                    final Set<ArtifactStore> updated = new HashSet<ArtifactStore>();
                    updateGroupsFor( store.getKey(), updated, true );
                }
                else
                {
                    try
                    {
                        final Set<TriggerKey> canceled =
                            scheduleManager.cancelAll( new StoreKeyMatcher( store.getKey(), REINDEX_JOB_TYPE ) );

                        scheduleManager.deleteJobs( canceled );
                    }
                    catch ( final AproxSchedulerException e )
                    {
                        logger.error( String.format( "Failed to cancel indexer trigger for: %s. Reason: %s", store,
                                                     e.getMessage() ), e );
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
                        logger.info( "Already updated group: {}", group );
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
