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
package org.commonjava.indy.content.index;

import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.core.expire.ContentExpiration;
import org.commonjava.indy.core.expire.ScheduleManager;
import org.commonjava.indy.core.expire.SchedulerEvent;
import org.commonjava.indy.core.expire.SchedulerEventType;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by jdcasey on 3/15/16.
 */
@ApplicationScoped
public class ContentIndexObserver
{
    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private ContentIndexManager indexManager;

    protected ContentIndexObserver()
    {
    }

    public ContentIndexObserver( StoreDataManager storeDataManager,
                                 ContentIndexManager indexManager,
                                 IndyObjectMapper objectMapper )
    {
        this.storeDataManager = storeDataManager;
        this.indexManager = indexManager;
        this.objectMapper = objectMapper;
    }

    public void onStoreDeletion( @Observes ArtifactStoreDeletePreEvent event )
    {
        for ( ArtifactStore store : event )
        {
            final StoreKey key = store.getKey();

            indexManager.removeAllIndexedPathsForStore( key );

            indexManager.removeAllOriginIndexedPathsForStore( key );
        }
    }

    public void onStoreUpdate( @Observes ArtifactStorePreUpdateEvent event )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Got event: {}", event );

        // we're only interested in existing stores, since new stores cannot have indexed keys
        if ( ArtifactStoreUpdateType.UPDATE == event.getType() )
        {
            for ( ArtifactStore store : event )
            {
                removeAllSupercededMemberContent( store, event.getChangeMap() );
            }
        }
    }

    public void invalidateExpiredContent( @Observes final SchedulerEvent event )
    {
        if ( event.getEventType() != SchedulerEventType.TRIGGER || !event.getJobType()
                                                                         .equals( ScheduleManager.CONTENT_JOB_TYPE ) )
        {
            return;
        }

        ContentExpiration expiration = null;
        try
        {
            expiration = objectMapper.readValue( event.getPayload(), ContentExpiration.class );
        }
        catch ( final IOException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( "Failed to read ContentExpiration from event payload.", e );
        }

        if ( expiration == null )
        {
            return;
        }

        final StoreKey key = expiration.getKey();
        final String path = expiration.getPath();

        // invalidate indexes for the store itself
        indexManager.removeIndexedStorePath( path, key );

        // invalidate indexes for groups containing the store
        indexManager.removeOriginIndexedStorePath( path, key );
    }

    private void removeAllSupercededMemberContent( ArtifactStore store, Map<ArtifactStore, ArtifactStore> changeMap )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        StoreKey key = store.getKey();
        // we're only interested in groups, since only adjustments to group memberships can invalidate indexed content.
        if ( StoreType.group == key.getType() )
        {
            List<StoreKey> newMembers = ( (Group) store ).getConstituents();
            logger.debug( "New members of: {} are: {}", store, newMembers );

            Group group = (Group) changeMap.get( store );
            List<StoreKey> oldMembers = group.getConstituents();
            logger.debug( "Old members of: {} are: {}", group, oldMembers );

            int commonSize = Math.min( newMembers.size(), oldMembers.size() );
            int divergencePoint = -1;

            // look in the members that overlap in new/old groups and see if there are changes that would
            // indicate member reordering. If so, it might lead previously suppressed results to be prioritized,
            // which would invalidate part of the content index for the group.
            for ( divergencePoint = 0; divergencePoint < commonSize; divergencePoint++ )
            {
                logger.debug( "Checking for common member at index: {}", divergencePoint );
                if ( !oldMembers.get( divergencePoint ).equals( newMembers.get( divergencePoint ) ) )
                {
                    break;
                }
            }

            // if we haven't found a reordering of membership, let's look to see if membership has shrunk
            // if it has just grown, we don't care.
            if ( divergencePoint < 0 && newMembers.size() < oldMembers.size() )
            {
                divergencePoint = commonSize;
            }

            logger.debug( "group membership divergence point: {}", divergencePoint );

            // if we can iterate some old members that have been removed or reordered, invalidate the
            // group content index entries for those.
            if ( divergencePoint < oldMembers.size() )
            {
                for ( int i = divergencePoint; i < oldMembers.size(); i++ )
                {
                    StoreKey memberKey = oldMembers.get( i );
                    indexManager.removeAllOriginIndexedPathsForStore( memberKey );
                }
            }
        }
    }

}
