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

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.prefetch.conf.PrefetchConfig;
import org.commonjava.indy.subsys.prefetch.models.RescanablePath;
import org.commonjava.indy.subsys.prefetch.models.RescanableResourceWrapper;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ConcreteResource;
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
import java.util.concurrent.Executor;

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

    @WeftManaged
    @ExecutorConfig( named = "prefetch-worker", threads = 5, priority = 1 )
    @Inject
    private Executor executor;

    private final Thread schedulingThread = new Thread( () -> {
        while ( true )
        {
            if ( !frontier.hasMore() )
            {
                frontier.rescheduleForRescan();
                triggerWorkers();
            }
            try
            {
                Thread.sleep( 500 );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        }
    }, "Prefetch-Scheduling" );

    @PostConstruct
    public void initPrefetch()
    {
        if(config.isEnabled())
        {
            frontier.initRepoCache();
            logger.trace( "PrefetchManager Started" );
            schedulingThread.setDaemon( true );
            logger.trace( "Scheduling thread start" );
            schedulingThread.start();

//            triggerWorkers();
        }
    }

    public void registerPrefetchStores( @Observes final ArtifactStorePostUpdateEvent updateEvent )
    {
        if(config.isEnabled())
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
                executor.execute( new PrefetchWorker( transfers, frontier, resources, PrefetchManager.this,
                                                      specialPathManager ) );
            }
        }
    }



    @PreDestroy
    public void stopPrefeching()
    {
        if ( config.isEnabled() )
        {
            frontier.stopSchedulingMore();
            schedulingThread.interrupt();
        }
    }
}
