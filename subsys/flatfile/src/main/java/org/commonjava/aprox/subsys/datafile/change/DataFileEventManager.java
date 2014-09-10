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
package org.commonjava.aprox.subsys.datafile.change;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.commonjava.aprox.audit.ChangeSummary;

/**
 * Helper class to provide simple methods to handle null-checking, etc. around the firing of AProx filesystem events.
 */
@ApplicationScoped
public class DataFileEventManager
{

    @Inject
    private Event<DataFileEvent> events;

    public void fire( final DataFileEvent evt )
    {
        if ( events != null )
        {
            events.fire( evt );
        }
    }

    public void accessed( final File file )
    {
        fire( new DataFileEvent( file ) );
    }

    public void modified( final File file, final ChangeSummary summary )
    {
        fire( new DataFileEvent( file, DataFileEventType.modified, summary ) );
    }

    public void deleted( final File file, final ChangeSummary summary )
    {
        fire( new DataFileEvent( file, DataFileEventType.deleted, summary ) );
    }
}
