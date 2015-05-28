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
package org.commonjava.aprox.core.change;

import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.util.ChangeSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class GroupConsistencyListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager proxyDataManager;

    private final ChangeSynchronizer changeSync = new ChangeSynchronizer();

    private void processChanged( final ArtifactStore store )
    {
        final StoreKey key = store.getKey();
        try
        {
            final Set<Group> groups = proxyDataManager.getGroupsContaining( key );
            for ( final Group group : groups )
            {
                group.removeConstituent( key );
                proxyDataManager.storeArtifactStore( group, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                       "Auto-update groups containing: " + key
                                                                                   + " (to maintain consistency)" ),
                                                     false, false );
            }

            changeSync.setChanged();
        }
        catch ( final AproxDataException e )
        {
            logger.error( String.format( "Failed to remove group constituent listings for: %s. Error: %s", key,
                                         e.getMessage() ), e );
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

    public void storeDeleted( @Observes final ArtifactStoreDeletePostEvent event )
    {
        //        logger.info( "Processing proxy-manager store deletion: {}", event );
        for ( final ArtifactStore store : event )
        {
            processChanged( store );
        }
    }

    public void waitForChange( final long total, final long poll )
    {
        changeSync.waitForChange( 1, total, poll );
    }

}
