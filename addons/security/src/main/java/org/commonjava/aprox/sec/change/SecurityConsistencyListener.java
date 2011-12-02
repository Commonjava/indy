/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.commonjava.couch.change.CouchDocChange;
import org.commonjava.couch.change.dispatch.ThreadableListener;
import org.commonjava.couch.rbac.Permission;
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
