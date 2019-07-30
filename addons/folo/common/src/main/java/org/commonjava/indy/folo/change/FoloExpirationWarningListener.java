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
