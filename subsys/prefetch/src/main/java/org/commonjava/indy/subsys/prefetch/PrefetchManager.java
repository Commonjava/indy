/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.subsys.prefetch;

import org.commonjava.cdi.util.weft.NamedThreadFactory;
import org.commonjava.indy.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.prefetch.conf.PrefetchConfig;
import org.commonjava.indy.subsys.prefetch.models.RescanablePath;
import org.commonjava.indy.subsys.prefetch.models.RescanableResourceWrapper;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class PrefetchManager
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private TransferManager transfers;

    @Inject
    private PrefetchFrontier frontier;

    @Inject
    private PrefetchConfig config;

    @Inject
    private SpecialPathManager specialPathManager;

    private volatile boolean stopped;

    private final ExecutorService prefetchExecutor =
            Executors.newFixedThreadPool( 5, new NamedThreadFactory( "Prefetch-Worker", true, 1 ) );

    private final ExecutorService rescanScheduleExecutor =
            Executors.newSingleThreadExecutor( new NamedThreadFactory( "Prefetch-Rescan-Scheduler", true, 1 ) );

    private final Runnable rescanSchedulingTask = () -> {
        if ( config.isEnabled() )
        {
            while ( !stopped )
            {
                if ( !frontier.hasMore() )
                {
                    frontier.rescheduleForRescan();
                    //TODO need to think use a flag to control the triggerWorkers(), and not invoke it in main thread in registerPrefetchStores
                    triggerWorkers();
                }
                try
                {
                    Thread.sleep( config.getRescanScheduleSeconds() );
                }
                catch ( InterruptedException e )
                {
                    logger.warn(
                            "Prefetch scheduling daemon has been interrupted. No more rescan enabled remote repos will be triggered for further downloading. " );
                }
            }
            logger.warn(
                    "Prefetch module has been stopped due to some reasons(maybe indy shut down). No more rescan enabled remote repos will be triggered for further downloading. " );
        }
    };

    @PostConstruct
    public void initPrefetch()
    {
        if ( config.isEnabled() )
        {
            stopped = false;
            frontier.initRepoCache();
            logger.trace( "PrefetchManager Started" );
            rescanScheduleExecutor.execute( rescanSchedulingTask );
//            final Thread scheduleThread = new Thread( rescanSchedulingTask );
//            scheduleThread.setDaemon( true );
//            scheduleThread.setPriority( 1 );
//            scheduleThread.setName( "Prefetch-Rescan-Scheduler"  );
//            scheduleThread.start();
        }
    }

    public void registerPrefetchStores( @Observes final ArtifactStorePostUpdateEvent updateEvent )
    {
        if ( config.isEnabled() )
        {
            logger.trace( "Post update triggered for scheduling of prefetch: {}", updateEvent );
            final Collection<ArtifactStore> stores = updateEvent.getStores();
            boolean scheduled = false;
            for ( ArtifactStore changedStore : stores )
            {
                if ( changedStore.getType() == StoreType.remote )
                {
                    ArtifactStore origStore = updateEvent.getOriginal( changedStore );
                    final RemoteRepository changedRemote = (RemoteRepository) changedStore;
                    final RemoteRepository origRemote = (RemoteRepository ) origStore;
                    final boolean remotePrefetchEnabled = ( origRemote != null && !origRemote.getPrefetchPriority()
                                                                                             .equals(
                                                                                                     changedRemote.getPrefetchPriority() ) )
                            && changedRemote.getPrefetchPriority() > 0;
                    if ( remotePrefetchEnabled )
                    {
                        List<RescanablePath> paths = frontier.buildPaths( changedRemote, false );
                        logger.trace( "Schedule resources: repo: {}, paths {}", changedRemote, paths );
                        frontier.scheduleRepo( changedRemote, paths );
                        scheduled = true;
                    }
                }
            }
            if ( scheduled )
            {
                triggerWorkers();
            }
        }
    }

    void triggerWorkers()
    {
        logger.trace( "Trigger works now" );

        //TODO: should use a separated thread here to loop this resource working to avoid main thread holding here.
        while ( frontier.hasMore() )
        {
            Map<RemoteRepository, List<RescanableResourceWrapper>> resources = frontier.remove( config.getBatchSize() );
            logger.trace( "Start to trigger threads to download {}", resources );
            boolean hasResourceDownload = false;
            for ( Map.Entry<RemoteRepository, List<RescanableResourceWrapper>> resource : resources.entrySet() )
            {
                if ( resource.getValue() != null && !resource.getValue().isEmpty() )
                {
                    hasResourceDownload = true;
                    break;
                }
            }
            if ( hasResourceDownload )
            {
                prefetchExecutor.execute( new PrefetchWorker( transfers, frontier, resources, PrefetchManager.this,
                                                              specialPathManager ) );
            }
        }
    }


    @PreDestroy
    public void stopPrefeching()
    {
        if ( config.isEnabled() )
        {
            stopped = true;
            frontier.stopSchedulingMore();
            prefetchExecutor.shutdown();
            rescanScheduleExecutor.shutdown();
            logger.info( "Indy prefetch process has been set to stopped. " );
        }
    }
}
