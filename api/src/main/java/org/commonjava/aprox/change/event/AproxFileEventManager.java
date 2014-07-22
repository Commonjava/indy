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
