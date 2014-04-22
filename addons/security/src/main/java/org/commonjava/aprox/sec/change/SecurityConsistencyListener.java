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
package org.commonjava.aprox.sec.change;

import java.util.Collection;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.sec.data.AProxSecDataManager;
import org.commonjava.aprox.util.ChangeSynchronizer;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class SecurityConsistencyListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private AProxSecDataManager dataManager;

    @Inject
    private ChangeSynchronizer changeSync;

    public void storeDeleted( @Observes final ProxyManagerDeleteEvent event )
    {
        logger.info( "\n\n\n\nProcessing JEE change notification: {}\n\n\n\n", event );
        final StoreType type = event.getType();
        final Collection<String> names = event.getNames();
        for ( final String name : names )
        {
            dataManager.deleteStorePermissions( type, name );
        }
    }

    public void waitForChange( final long totalMillis, final long pollingMillis )
    {
        changeSync.waitForChange( 1, totalMillis, pollingMillis );
    }

}
