/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.change;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Set;

@javax.enterprise.context.ApplicationScoped
public class GroupConsistencyListener
{

    public static final String GROUP_CONSISTENCY_ORIGIN = "group-consistency";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager storeDataManager;

    private void processChanged( final ArtifactStore store )
    {
        final StoreKey key = store.getKey();
        try
        {
            final Set<Group> groups = storeDataManager.query().getGroupsContaining( key );
            logger.trace( "For repo: {}, containing groups are: {}", key, groups );
            for ( final Group group : groups )
            {
                logger.debug( "Removing {} from membership of group: {}", key, group.getKey() );

                Group g = group.copyOf();
                g.removeConstituent( key );
                storeDataManager.storeArtifactStore( g, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                           "Auto-update groups containing: " + key
                                                                                   + " (to maintain consistency)" ),
                                                     false, false,
                                                     new EventMetadata().set( StoreDataManager.EVENT_ORIGIN,
                                                                              GROUP_CONSISTENCY_ORIGIN ) );
            }
        }
        catch ( final IndyDataException e )
        {
            logger.error( String.format( "Failed to remove group constituent listings for: %s. Error: %s", key,
                                         e.getMessage() ), e );
        }
    }

    public void storeDeleted( @Observes final ArtifactStoreDeletePostEvent event )
    {
        logger.trace( "Processing proxy-manager store deletion: {}", event.getStores() );
        for ( final ArtifactStore store : event )
        {
            logger.trace( "Processing deletion of: {}", store.getKey() );
            processChanged( store );
        }
    }

}
