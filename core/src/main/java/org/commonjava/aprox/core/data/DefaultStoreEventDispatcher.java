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
package org.commonjava.aprox.core.data;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.aprox.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateEvent;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateType;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.data.StoreEventDispatcher;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.maven.galley.model.Transfer;

public class DefaultStoreEventDispatcher
    implements StoreEventDispatcher
{

    @Inject
    private Event<ArtifactStoreUpdateEvent> storeEvent;

    @Inject
    private Event<ArtifactStoreDeletePreEvent> preDelEvent;

    @Inject
    private Event<ArtifactStoreDeletePostEvent> postDelEvent;

    @Inject
    private DownloadManager fileManager;

    @Override
    public void deleting( final ArtifactStore... stores )
    {
        if ( preDelEvent != null )
        {
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

            final ArtifactStoreDeletePreEvent event = new ArtifactStoreDeletePreEvent( storeRoots );

            preDelEvent.fire( event );
        }
    }

    @Override
    public void deleted( final ArtifactStore... stores )
    {
        if ( postDelEvent != null )
        {
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

            final ArtifactStoreDeletePostEvent event = new ArtifactStoreDeletePostEvent( storeRoots );

            postDelEvent.fire( event );
        }
    }

    @Override
    public void updating( final ArtifactStoreUpdateType type, final ArtifactStore... stores )
    {
        if ( storeEvent != null )
        {
            final ArtifactStoreUpdateEvent event = new ArtifactStoreUpdateEvent( type, stores );
            storeEvent.fire( event );
        }
    }

}
