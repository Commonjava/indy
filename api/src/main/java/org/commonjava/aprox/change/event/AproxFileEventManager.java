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
package org.commonjava.aprox.change.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileErrorEvent;
import org.commonjava.maven.galley.event.FileNotFoundEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;

/**
 * Helper class to provide simple methods to handle null-checking, etc. around the firing of AProx filesystem events.
 */
@ApplicationScoped
public class AproxFileEventManager
    implements org.commonjava.maven.galley.spi.event.FileEventManager
{

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

    @Override
    public void fire( final FileNotFoundEvent evt )
    {
        doFire( notFoundEvent, evt );
    }

    @Override
    public void fire( final FileStorageEvent evt )
    {
        doFire( storageEvent, evt );
    }

    @Override
    public void fire( final FileAccessEvent evt )
    {
        doFire( accessEvent, evt );
    }

    @Override
    public void fire( final FileDeletionEvent evt )
    {
        doFire( deleteEvent, evt );
    }

    @Override
    public void fire( final FileErrorEvent evt )
    {
        doFire( errorEvent, evt );
    }

    private <T> void doFire( final Event<T> eventQ, final T evt )
    {
        if ( eventQ != null )
        {
            eventQ.fire( evt );
        }
    }
}
