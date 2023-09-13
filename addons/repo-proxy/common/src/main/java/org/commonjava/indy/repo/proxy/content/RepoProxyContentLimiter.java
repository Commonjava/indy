/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.repo.proxy.content;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.repo.proxy.conf.RepoProxyConfig;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryExpiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Map;

import static org.commonjava.indy.model.core.StoreType.remote;

@ApplicationScoped
@Listener
public class RepoProxyContentLimiter
{

    private final CacheHandle<String, String> storedPaths;

    private final RepoProxyConfig config;

    private final StoreDataManager storeDataManager;

    private final ContentManager contentManager;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    public RepoProxyContentLimiter( @RepoProxyContentCache CacheHandle<String, String> storedPaths,
                                    StoreDataManager storeDataManager, ContentManager contentManager,
                                    RepoProxyConfig config )
    {
        this.storedPaths = storedPaths;
        this.storeDataManager = storeDataManager;
        this.contentManager = contentManager;
        this.config = config;
    }

    public void onContentStorage( @Observes FileStorageEvent storageEvent )
    {
        if ( !isEnabled() )
        {
            return;
        }

        StoreKey storeKey = LocationUtils.getKey( storageEvent );
        if ( remote == storeKey.getType() )
        {
            String path = storageEvent.getTransfer().getPath();
            String skp = storeKey.toString() + "#" + path;

            storedPaths.put( skp, skp );
        }
    }

    private boolean isEnabled()
    {
        return config.isEnabled() && config.isContentLimiterEnabled();
    }

    @CacheEntryExpired
    public void onContentExpiration( CacheEntryExpiredEvent<String, String> event )
    {
        if ( !isEnabled() )
        {
            return;
        }

        clearContent( event.getKey() );
    }

    @CacheEntriesEvicted
    public void onContentEviction( CacheEntriesEvictedEvent<String, String> event )
    {
        if ( !isEnabled() )
        {
            return;
        }

        Map<String, String> entries = event.getEntries();
        for ( String skp : entries.keySet() )
        {
            clearContent( skp );
        }
    }

    private void clearContent( String skp )
    {
        String[] parts = skp.split( "#" );
        StoreKey storeKey = StoreKey.fromString( parts[0] );

        if ( remote == storeKey.getType() )
        {
            ArtifactStore store;
            try
            {
                store = storeDataManager.getArtifactStore( storeKey );
                contentManager.delete( store, parts[1] );
            }
            catch ( IndyDataException e )
            {
                logger.warn( "Failed to lookup store: {} from event: {}", storeKey, skp );
            }
            catch ( IndyWorkflowException e )
            {
                logger.warn( "Failed to delete: {} from: {}, from event: {}", parts[1], storeKey, skp );
            }
        }
    }

}
