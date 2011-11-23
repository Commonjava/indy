/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.sec.change;

import java.util.Collection;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.auth.couch.data.UserDataException;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.auth.couch.model.Permission;
import org.commonjava.couch.change.CouchDocChange;
import org.commonjava.couch.change.dispatch.ThreadableListener;
import org.commonjava.couch.util.ChangeSynchronizer;
import org.commonjava.util.logging.Logger;

@Singleton
public class SecurityConsistencyListener
    implements ThreadableListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private UserDataManager userDataManager;

    private final ChangeSynchronizer changeSync = new ChangeSynchronizer();

    @Override
    public boolean canProcess( final String id, final boolean deleted )
    {
        logger.info( "\n\n\n\nProcessing change: %s (deleted? %b)\n\n\n\n", id, deleted );
        return deleted
            && ( id.startsWith( StoreType.repository.name() ) || id.startsWith( StoreType.deploy_point.name() ) || id.startsWith( StoreType.group.name() ) );
    }

    @Override
    public void documentChanged( final CouchDocChange change )
    {
        final String id = change.getId();
        try
        {
            logger.info( "\n\n\n\nDeleting permissions for group: %s\n\n\n\n", id );
            userDataManager.deletePermission( Permission.name( id, Permission.ADMIN ) );
            userDataManager.deletePermission( Permission.name( id, Permission.READ ) );

            changeSync.setChanged();
        }
        catch ( final UserDataException e )
        {
            logger.error( "Failed to remove permissions for deleted store: %s. Error: %s", e, id, e.getMessage() );
        }
    }

    public void storeDeleted( @Observes final ProxyManagerDeleteEvent event )
    {
        logger.info( "\n\n\n\nProcessing JEE change notification: %s\n\n\n\n", event );
        final StoreType type = event.getType();
        final Collection<String> names = event.getChanges();
        for ( final String name : names )
        {
            try
            {
                logger.info( "\n\n\n\nDeleting permissions for store: %s:%s\n\n\n\n", type.name(), name );
                userDataManager.deletePermission( Permission.name( type.name(), name, Permission.ADMIN ) );
                userDataManager.deletePermission( Permission.name( type.name(), name, Permission.READ ) );

                changeSync.setChanged();
            }
            catch ( final UserDataException e )
            {
                logger.error( "Failed to remove permissions for deleted store: %s:%s. Error: %s", e, type.name(), name,
                              e.getMessage() );
            }
        }
    }

    @Override
    public void waitForChange( final long totalMillis, final long pollingMillis )
    {
        changeSync.waitForChange( totalMillis, pollingMillis );
    }

}
