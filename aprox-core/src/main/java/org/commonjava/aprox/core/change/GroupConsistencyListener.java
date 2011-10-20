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
package org.commonjava.aprox.core.change;

import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.couch.change.CouchDocChange;
import org.commonjava.couch.change.dispatch.CouchChangeJ2EEEvent;
import org.commonjava.couch.change.dispatch.ThreadableListener;
import org.commonjava.couch.util.ChangeSynchronizer;
import org.commonjava.util.logging.Logger;

@Singleton
public class GroupConsistencyListener
    implements ThreadableListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyDataManager;

    private final ChangeSynchronizer changeSync = new ChangeSynchronizer();

    @Override
    public boolean canProcess( final String id, final boolean deleted )
    {
        return deleted
            && ( id.startsWith( StoreType.repository.name() )
                || id.startsWith( StoreType.deploy_point.name() ) || id.startsWith( StoreType.group.name() ) );
    }

    @Override
    public void documentChanged( final CouchDocChange change )
    {
        String id = change.getId();
        StoreKey key = StoreKey.fromString( id );
        try
        {
            Set<Group> groups = proxyDataManager.getGroupsContaining( key );
            for ( Group group : groups )
            {
                group.removeConstituent( StoreKey.fromString( id ) );
            }

            proxyDataManager.storeGroups( groups );

            changeSync.setChanged();
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to remove group constituent listings for: %s. Error: %s", e, id,
                          e.getMessage() );
        }
    }

    public void storeDeleted( @Observes final CouchChangeJ2EEEvent event )
    {
        CouchDocChange change = event.getChange();
        if ( canProcess( change.getId(), change.isDeleted() ) )
        {
            documentChanged( change );
        }
    }

    @Override
    public void waitForChange( final long totalMillis, final long pollingMillis )
    {
        changeSync.waitForChange( totalMillis, pollingMillis );
    }

}
