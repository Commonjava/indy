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
package org.commonjava.indy.folo.change;

import org.commonjava.indy.folo.ctl.FoloAdminController;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackingKey;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;

/**
 * We need to protect against cache deletion/crash for sealed records, just to be safe.
 * We need to look at either sending the serialized record to the UMB or else just storing on disk somewhere
 * outside the ISPN directory.
 *
 * Update: For now let's push it to disk somewhere and worry about more advanced things later.
 */
@Listener
@ApplicationScoped
public class FoloBackupListener
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private FoloAdminController adminController;

    public FoloBackupListener( FoloAdminController adminController )
    {
        this.adminController = adminController;
    }

    public FoloBackupListener()
    {
    }

    @CacheEntryCreated
    public void onCacheEntryCreated( final CacheEntryCreatedEvent<TrackingKey, TrackedContent> event )
    {
        if ( event.isPre() )
        {
            return;
        }
        logger.debug( "Cache entry with key {} added in cache {}", event.getKey(), event.getCache() );
        try
        {
            adminController.saveToSerialized( event.getKey(), event.getValue() );
        }
        catch ( IOException e )
        {
            logger.warn( "[Folo] saveToSerialize fail", e );
        }
    }

    @CacheEntryModified
    public void onCacheEntryModified( CacheEntryModifiedEvent<TrackingKey, TrackedContent> event )
    {
        if ( event.isPre() )
        {
            return;
        }
        logger.debug( "Cache entry with key {} updated in cache {}", event.getKey(), event.getCache() );
        try
        {
            adminController.saveToSerialized( event.getKey(), event.getValue() );
        }
        catch ( IOException e )
        {
            logger.warn( "[Folo] saveToSerialize fail", e );
        }
    }

    @CacheEntryRemoved
    public void onCacheEntryRemoved( final CacheEntryRemovedEvent<TrackingKey, TrackedContent> event )
    {
        if ( event.isPre() )
        {
            return;
        }

        TrackingKey key = event.getKey();

        logger.debug( "Cache entry with key {} removed in cache {}", key, event.getCache() );
        adminController.removeFromSerialized( key );
    }

}
