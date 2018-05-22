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
import org.commonjava.maven.galley.TransferManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@ApplicationScoped
public class PrefetchManager
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private TransferManager transfers;

    @Inject
    private PrefetchFrontier frontier;

    @WeftManaged
    @ExecutorConfig( named = "prefetch-worker", threads = 5, priority = 1 )
    @Inject
    private Executor executor;

    private static final int BATCH_SIZE = 10;

    @Inject
    private Instance<ContentListBuilder> listBuilders;

    @PostConstruct
    public void initPrefetch()
    {
        logger.info( "PrefetchManager Started" );
        triggerWorkers();
    }

    public void registerPrefetchStores( @Observes final ArtifactStorePostUpdateEvent updateEvent )
    {
        logger.trace( "Post update triggered for scheduling of prefetch: {}", updateEvent );
        Collection<ArtifactStore> stores = updateEvent.getStores();
        boolean scheduled = false;
        for ( ArtifactStore store : stores )
        {
            if ( store.getType() == StoreType.remote )
            {
                RemoteRepository remote = (RemoteRepository) store;
                if ( remote.getPrefetchPriority() > 0 )
                {
                    logger.trace( "Store updated for scheduling of prefetch: {}", store );
                    List<PrioritizedResource> resources = buildResources( remote );
                    logger.trace( "Schedule resources {}", resources );
                    frontier.scheduleResources( resources );
                    scheduled = true;
                }
            }
        }
        if ( scheduled )
        {
            triggerWorkers();
        }
    }

    void triggerWorkers()
    {
        logger.info( "Trigger works now" );
        while ( frontier.hasMore() )
        {
            List<PrioritizedResource> resources = frontier.remove( BATCH_SIZE );
            logger.trace( "Start to trigger threads to download {}", resources );
            executor.execute( new PrefetchWorker( transfers, frontier, resources, PrefetchManager.this, logger ) );
        }
    }

    private List<PrioritizedResource> buildResources( final RemoteRepository repository )
    {
        for ( ContentListBuilder builder : listBuilders )
        {
            if ( repository.getPrefetchListingType().equals( builder.type() ) )
            {
                logger.trace( "Use {} for {}", builder, repository.getName() );
                return builder.buildContent( repository )
                              .stream()
                              .map( s -> new PrioritizedResource( s, repository.getPrefetchPriority() ) )
                              .collect( Collectors.toList() );
            }
        }
        return Collections.emptyList();
    }

    @PreDestroy
    public void stopPrefeching(){
        frontier.stopSchedulingMore();
    }
}
