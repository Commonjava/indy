/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.content.index.deindex;

import org.commonjava.indy.content.index.ContentIndexManager;
import org.commonjava.indy.content.index.conf.ContentIndexConfig;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

import static org.commonjava.indy.content.index.deindex.DeIndexInfo.TYPE_MULTI;
import static org.commonjava.indy.content.index.deindex.DeIndexInfo.TYPE_SINGLE;

@Listener( sync = false )
@ApplicationScoped
public class DeIndexHandlingCacheListener
{
    private static final Logger logger = LoggerFactory.getLogger( DeIndexHandlingCacheListener.class );

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private ContentIndexManager indexManager;

    @Inject
    private ContentIndexConfig indexCfg;

    @Inject
    private NotFoundCache nfc;

    @Inject
    @DeIndexHandlingCache
    private CacheHandle<Long, DeIndexInfo> deIndexQueue;

    @CacheEntryCreated
    public void handleDeIndex( CacheEntryCreatedEvent<Long, DeIndexInfo> e )
    {
        if ( !e.isPre() )
        {
            final DeIndexInfo info = e.getValue();
            final String deIndexType = info.deIndexType;
            switch ( deIndexType )
            {
                case TYPE_SINGLE:
                    processSingle( info );
                    break;
                case TYPE_MULTI:
                    processMultiple( info );
                    break;
                default:
                    break;
            }
        }
        deIndexQueue.remove( e.getKey() );
    }

    private void processSingle( final DeIndexInfo info )
    {
        final Transfer transfer = info.transfer;
        final ArtifactStore store = info.store;
        final String path = transfer.getPath();

        logger.trace( "Do de-index in storing process of content-index for transfer {} and store {}", transfer, store.getKey() );

        indexTransfer( transfer, store.getKey() );

        if ( store instanceof Group )
        {
            nfc.clearMissing( new ConcreteResource( LocationUtils.toLocation( store ), path ) );
        }
        // We should deIndex the path for all parent groups because the new content of the path
        // may change the content index sequence based on the constituents sequence in parent groups
        if ( store.getType() == StoreType.hosted )
        {
            try
            {
                Set<Group> groups = storeDataManager.query().getGroupsAffectedBy( store.getKey() );
                if ( groups != null && !groups.isEmpty() && indexCfg.isEnabled() )
                {
                    groups.forEach( g -> indexManager.deIndexStorePath( g.getKey(), path ) );
                }
            }
            catch ( IndyDataException e )
            {
                logger.error( String.format( "Failed to get groups which contains: %s for NFC handling. Reason: %s",
                                             store.getKey(), e.getMessage() ), e );
            }
        }
    }

    private void processMultiple( final DeIndexInfo info )
    {
        final Transfer transfer = info.transfer;
        final StoreKey topKey = info.topKey;
        final String path = transfer.getPath();

        logger.trace( "Do multiple de-index in storing process of content-index for transfer {} and top store {}", transfer, topKey );

        indexTransfer( transfer, topKey );

        try
        {
            ArtifactStore topStore = storeDataManager.getArtifactStore( topKey );
            nfc.clearMissing( new ConcreteResource( LocationUtils.toLocation( topStore ), path ) );

            if ( indexCfg.isEnabled() )
            {
                // We should deIndex the path for all parent groups because the new content of the path
                // may change the content index sequence based on the constituents sequence in parent groups
                indexManager.deIndexStorePath( topKey, path );
            }
        }
        catch ( IndyDataException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( String.format( "Failed to retrieve top store: %s for NFC management. Reason: %s", topKey,
                                         e.getMessage() ), e );
        }
    }

    private void indexTransfer( final Transfer transfer, final StoreKey key )
    {
        if ( indexCfg.isEnabled() )
        {
            logger.trace( "Indexing: {} in: {}", transfer, key );
            indexManager.indexTransferIn( transfer, key );
        }
    }
}
