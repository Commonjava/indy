/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.change.event;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStoreEnablementEvent;
import org.commonjava.indy.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.change.event.CoreEventManagerConstants;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static org.commonjava.indy.change.EventUtils.fireEvent;

/**
 * Pre-events (deleting, updating, enabling) are single-threaded (inline to user thread) so there isn't a race
 * condition between their execution and the target action.
 * For post-events (deleted, updated, enabled), it's intended that those are less critical and can happen async.
 *
 * This also makes them in right order - pre before action and post after action.
 */
public class DefaultStoreEventDispatcher
        implements StoreEventDispatcher
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Event<ArtifactStorePreUpdateEvent> updatePreEvent;

    @Inject
    private Event<ArtifactStorePostUpdateEvent> updatePostEvent;

    @Inject
    private Event<ArtifactStoreDeletePreEvent> preDelEvent;

    @Inject
    private Event<ArtifactStoreDeletePostEvent> postDelEvent;

    @Inject
    private Event<ArtifactStoreEnablementEvent> enablementEvent;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = CoreEventManagerConstants.DISPATCH_EXECUTOR_NAME,
                     threads = CoreEventManagerConstants.DISPATCH_EXECUTOR_THREADS,
                     priority = CoreEventManagerConstants.DISPATCH_EXECUTOR_PRIORITY )
    private Executor executor;

    @Inject
    private DownloadManager fileManager;

    @Override
    public void deleting( final EventMetadata eventMetadata, final ArtifactStore... stores )
    {
        if ( preDelEvent != null )
        {
            logger.trace( "Dispatch pre-delete event for: {}", Arrays.asList( stores ) );

            final Map<ArtifactStore, Transfer> storeRoots = new HashMap<>();
            for ( final ArtifactStore store : stores )
            {
                if ( store == null )
                {
                    continue;
                }

                final Transfer root = fileManager.getStoreRootDirectory( store );
                storeRoots.put( store, root );
            }

            final ArtifactStoreDeletePreEvent event = new ArtifactStoreDeletePreEvent( eventMetadata, storeRoots );

            fireEvent( preDelEvent, event );
        }
    }

    @Override
    public void deleted( final EventMetadata eventMetadata, final ArtifactStore... stores )
    {
        if ( postDelEvent != null )
        {
            final List<StoreKey> storeList =
                    Arrays.asList( stores ).stream().map( store -> store.getKey() ).collect( Collectors.toList() );

            logger.trace( "Dispatch post-delete event for: {}", storeList );

            executor.execute( () -> {
                String oldName = Thread.currentThread().getName();
                try
                {
                    Thread.currentThread().setName( "POST-DELETE-EVENT: " + storeList );
                    final Map<ArtifactStore, Transfer> storeRoots = new HashMap<>();
                    for ( final ArtifactStore store : stores )
                    {
                        if ( store == null )
                        {
                            continue;
                        }

                        final Transfer root = fileManager.getStoreRootDirectory( store );
                        storeRoots.put( store, root );
                    }

                    final ArtifactStoreDeletePostEvent event =
                            new ArtifactStoreDeletePostEvent( eventMetadata, storeRoots );

                    fireEvent( postDelEvent, event );
                }
                finally
                {
                    if ( oldName != null )
                    {
                        Thread.currentThread().setName( oldName );
                    }
                }
            } );
        }
    }

    @Override
    public void updating( final ArtifactStoreUpdateType type, final EventMetadata eventMetadata,
                          final Map<ArtifactStore, ArtifactStore> changeMap )
    {
        final ArtifactStorePreUpdateEvent event =
                new ArtifactStorePreUpdateEvent( type, eventMetadata, changeMap );
        fireEvent( updatePreEvent, event );
    }

    @Override
    public void updated( final ArtifactStoreUpdateType type, final EventMetadata eventMetadata,
                         final Map<ArtifactStore, ArtifactStore> changeMap )
    {
        executor.execute( () -> {
            final ArtifactStorePostUpdateEvent event =
                    new ArtifactStorePostUpdateEvent( type, eventMetadata, changeMap );
            fireEvent( updatePostEvent, event );
        } );
    }

    @Override
    public void enabling( EventMetadata eventMetadata, ArtifactStore... stores )
    {
        logger.trace( "Dispatch pre-enable event for: {}", Arrays.asList( stores ) );

        fireEnablement( true, eventMetadata, false, stores );
    }

    @Override
    public void enabled( EventMetadata eventMetadata, ArtifactStore... stores )
    {
        logger.trace( "Dispatch post-enable event for: {}", Arrays.asList( stores ) );

        executor.execute( ()->{
            fireEnablement( false, eventMetadata, false, stores );
        } );
    }

    @Override
    public void disabling( EventMetadata eventMetadata, ArtifactStore... stores )
    {
        fireEnablement( true, eventMetadata, true, stores );
    }

    @Override
    public void disabled( EventMetadata eventMetadata, ArtifactStore... stores )
    {
        executor.execute( ()->{
            fireEnablement( false, eventMetadata, true, stores );
        } );
    }

    private void fireEnablement( boolean preprocess, EventMetadata eventMetadata, boolean disabling,
                                 ArtifactStore... stores )
    {
        if ( enablementEvent != null )
        {
            final ArtifactStoreEnablementEvent event =
                    new ArtifactStoreEnablementEvent( preprocess, eventMetadata, disabling, stores );

            if ( preprocess )
            {
                fireEvent( enablementEvent, event );
            }
            else
            {
                executor.execute( ()->{
                    fireEvent( enablementEvent, event );
                });
            }
        }
    }

}
