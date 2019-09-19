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

import org.commonjava.indy.change.event.IndyStoreErrorEvent;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileErrorEvent;
import org.commonjava.maven.galley.event.FileNotFoundEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import static org.commonjava.indy.change.EventUtils.fireEvent;

/**
 * Helper class to provide simple methods to handle null-checking, etc. around the firing of Indy filesystem events.
 */
@ApplicationScoped
public class IndyFileEventManager
        implements org.commonjava.maven.galley.spi.event.FileEventManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Event<FileStorageEvent> storageEvent;

    @Inject
    private Event<FileAccessEvent> accessEvent;

    @Inject
    private Event<FileDeletionEvent> deleteEvent;

    @Inject
    private Event<FileErrorEvent> errorEvent;

    @Inject
    private Event<FileNotFoundEvent> notFoundEvent;

    @Inject
    private Event<IndyStoreErrorEvent> storeErrorEvent;

//    @ExecutorConfig( named = CoreEventManagerConstants.DISPATCH_EXECUTOR_NAME,
//                     threads = CoreEventManagerConstants.DISPATCH_EXECUTOR_THREADS,
//                     priority = CoreEventManagerConstants.DISPATCH_EXECUTOR_PRIORITY )
//    @WeftManaged
//    @Inject
//    private Executor executor;

    @Override
    public void fire( final FileNotFoundEvent evt )
    {
        if ( shouldFireEvent( evt.getEventMetadata() ) ){
            fireEvent( notFoundEvent, evt );
        }
    }

    @Override
    public void fire( final FileStorageEvent evt )
    {
        if ( shouldFireEvent( evt.getEventMetadata() ) )
        {
            fireEvent( storageEvent, evt );
        }
    }

    @Override
    public void fire( final FileAccessEvent evt )
    {
        if ( shouldFireEvent( evt.getEventMetadata() ) )
        {
            fireEvent( accessEvent, evt );
        }
    }

    @Override
    public void fire( final FileDeletionEvent evt )
    {
        if ( shouldFireEvent( evt.getEventMetadata() ) )
        {
            fireEvent( deleteEvent, evt );
        }
    }

    @Override
    public void fire( final FileErrorEvent evt )
    {
        if ( shouldFireEvent( evt.getEventMetadata() ) )
        {
            fireEvent( errorEvent, evt );
        }
    }

    public void fire( final IndyStoreErrorEvent evt )
    {
        fireEvent( storeErrorEvent, evt );
    }

    private boolean shouldFireEvent( EventMetadata eventMetadata )
    {
        return ( eventMetadata == null || !Boolean.TRUE.equals( eventMetadata.get( ContentManager.SUPPRESS_EVENTS ) ) );
    }

}
