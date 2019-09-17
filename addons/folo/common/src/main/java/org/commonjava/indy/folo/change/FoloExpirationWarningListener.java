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

import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryExpiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;

@Listener
@ApplicationScoped
public class FoloExpirationWarningListener
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @CacheEntryExpired
    public void onCacheEntryExpired( final CacheEntryExpiredEvent<TrackedContentEntry, TrackedContentEntry> event )
    {
        if ( event.isPre() )
        {
            return;
        }

        TrackedContentEntry entry = event.getKey();
        logger.warn( "Tracking record entry {}:{}:{} was expired by Infinispan!", entry.getTrackingKey(), entry.getStoreKey(), entry.getPath() );
    }
}
