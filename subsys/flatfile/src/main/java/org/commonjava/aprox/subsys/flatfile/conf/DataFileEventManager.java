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
package org.commonjava.aprox.subsys.flatfile.conf;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.commonjava.aprox.subsys.flatfile.conf.change.DataFileAccessEvent;
import org.commonjava.aprox.subsys.flatfile.conf.change.DataFileDeletionEvent;
import org.commonjava.aprox.subsys.flatfile.conf.change.DataFileStorageEvent;

/**
 * Helper class to provide simple methods to handle null-checking, etc. around the firing of AProx filesystem events.
 */
@ApplicationScoped
public class DataFileEventManager
{

    @Inject
    private Event<DataFileStorageEvent> storageEvent;

    @Inject
    private Event<DataFileAccessEvent> accessEvent;

    @Inject
    private Event<DataFileDeletionEvent> deleteEvent;

    public void fire( final DataFileStorageEvent evt )
    {
        doFire( storageEvent, evt );
    }

    public void fire( final DataFileAccessEvent evt )
    {
        doFire( accessEvent, evt );
    }

    public void fire( final DataFileDeletionEvent evt )
    {
        doFire( deleteEvent, evt );
    }

    private <T> void doFire( final Event<T> eventQ, final T evt )
    {
        if ( eventQ != null )
        {
            eventQ.fire( evt );
        }
    }
}
