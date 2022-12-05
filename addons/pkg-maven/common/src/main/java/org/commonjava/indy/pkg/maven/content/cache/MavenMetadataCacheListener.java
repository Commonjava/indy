/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pkg.maven.content.cache;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.MetadataInfo;
import org.commonjava.indy.pkg.maven.content.MetadataKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryExpiredEvent;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.event.CacheEntryExpiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static org.commonjava.indy.data.StoreDataManager.IGNORE_READONLY;

@ApplicationScoped
@Listener
@ClientListener
public class MavenMetadataCacheListener
{

    @Inject
    private DownloadManager downloadManager;

    @Inject
    private StoreDataManager storeDataManager;

    @CacheEntryExpired
    public void metadataExpired( CacheEntryExpiredEvent<MetadataKey, MetadataInfo> event )
    {
        handleMetadataExpired( event.getKey() );
    }

    @ClientCacheEntryExpired
    public void metadataExpired( ClientCacheEntryExpiredEvent<MetadataKey> event )
    {
        handleMetadataExpired( event.getKey() );
    }

    private void handleMetadataExpired( MetadataKey key )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        StoreKey storeKey = key.getStoreKey();
        String path = key.getPath();

        try
        {
            ArtifactStore store = storeDataManager.getArtifactStore( storeKey );
            if ( store != null )
            {
                downloadManager.delete(store, path,
                        new EventMetadata().set(IGNORE_READONLY, true));
            }
        }
        catch (IndyWorkflowException | IndyDataException e )
        {
            logger.warn( "On cache expiration, metadata file deletion failed for: " + path + " in store: " + storeKey, e );
        }
    }

}
