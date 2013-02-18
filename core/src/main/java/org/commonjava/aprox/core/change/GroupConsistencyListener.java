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
package org.commonjava.aprox.core.change;

import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.change.event.ProxyManagerDeleteEvent;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.util.ChangeSynchronizer;
import org.commonjava.util.logging.Logger;

@javax.enterprise.context.ApplicationScoped
public class GroupConsistencyListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager proxyDataManager;

    private final ChangeSynchronizer changeSync = new ChangeSynchronizer();

    private void processChanged( final StoreKey key )
    {
        try
        {
            final Set<Group> groups = proxyDataManager.getGroupsContaining( key );
            for ( final Group group : groups )
            {
                group.removeConstituent( key );
            }

            proxyDataManager.storeGroups( groups );

            changeSync.setChanged();
        }
        catch ( final ProxyDataException e )
        {
            logger.error( "Failed to remove group constituent listings for: %s. Error: %s", e, key, e.getMessage() );
        }
    }

    // public void storeDeleted( @Observes final CouchChangeJ2EEEvent event )
    // {
    // final CouchDocChange change = event.getChange();
    // final String id = change.getId();
    //
    // final boolean canProcess =
    // change.isDeleted()
    // && ( id.startsWith( StoreType.repository.name() ) || id.startsWith( StoreType.deploy_point.name() ) ||
    // id.startsWith( StoreType.group.name() ) );
    //
    // if ( canProcess )
    // {
    // final StoreKey key = StoreKey.fromString( id );
    // processChanged( key );
    // }
    // }

    public void storeDeleted( @Observes final ProxyManagerDeleteEvent event )
    {
        //        logger.info( "Processing proxy-manager store deletion: %s", event );
        final StoreType type = event.getType();
        for ( final String name : event )
        {
            final StoreKey key = new StoreKey( type, name );
            processChanged( key );
        }
    }

    public void waitForChange( final long total, final long poll )
    {
        changeSync.waitForChange( 1, total, poll );
    }

}
