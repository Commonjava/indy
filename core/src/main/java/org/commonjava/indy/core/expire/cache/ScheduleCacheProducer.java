/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.core.expire.cache;

import org.commonjava.indy.core.expire.ScheduleManager;
import org.commonjava.indy.core.expire.SchedulerEvent;
import org.commonjava.indy.core.expire.SchedulerScheduleEvent;
import org.commonjava.indy.core.expire.SchedulerTriggerEvent;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryExpired;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryExpiredEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This ISPN cache producer is used to generate {@link ScheduleCache}. This type of cache is used as a job control center
 * to control the content expiration scheduling.
 * BTW, this class is also acted as a cache listener to handle the cache expiration job event to distribute the expiration
 * info through CDI event for the real actor to do the expiration work.
 */
@Listener
public class ScheduleCacheProducer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CacheProducer cacheProducer;

    private static final String SCHEDULE_EXPIRE = "schedule-expire-cache";

    @Inject
    private Event<SchedulerEvent> eventDispatcher;

    @PostConstruct
    public void initExpireConfig()
    {
        final Configuration c = new ConfigurationBuilder().expiration().wakeUpInterval( 1, TimeUnit.SECONDS ).build();
        cacheProducer.setCacheConfiguration( SCHEDULE_EXPIRE, c );
    }

    @ScheduleCache
    @Produces
    @ApplicationScoped
    public CacheHandle<String, Map> versionMetadataCache()
    {
        final CacheHandle<String, Map> cacheHandle = cacheProducer.getCache( SCHEDULE_EXPIRE, String.class, Map.class );
        // register this producer as schedule cache listener
        cacheHandle.execute( cache -> {
            cache.addListener( ScheduleCacheProducer.this );
            return null;
        } );
        return cacheHandle;
    }

    @CacheEntryCreated
    public void scheduled( final CacheEntryCreatedEvent<String, Map> e )
    {
        final String expiredKey = e.getKey();
        final Map expiredContent = e.getValue();
        if ( expiredKey != null && expiredContent != null )
        {
            logger.debug( "Expiration Created: {}", expiredKey );
            final String type = (String) expiredContent.get( ScheduleManager.JOB_TYPE );
            final String data = (String) expiredContent.get( ScheduleManager.PAYLOAD );
            eventDispatcher.fire( new SchedulerScheduleEvent( type, data ) );
        }
    }

    @CacheEntryExpired
    public void expired( CacheEntryExpiredEvent<String, Map> e )
    {
        final String expiredKey = e.getKey();
        final Map expiredContent = e.getValue();
        if ( expiredKey != null && expiredContent != null )
        {
            logger.debug( "EXPIRED: {}", expiredKey );
            final String type = (String) expiredContent.get( ScheduleManager.JOB_TYPE );
            final String data = (String) expiredContent.get( ScheduleManager.PAYLOAD );
            eventDispatcher.fire( new SchedulerTriggerEvent( type, data ) );
        }
    }

    @CacheEntryRemoved
    public void cancelled( CacheEntryRemovedEvent<String, Map> e )
    {
        logger.info( "Cache removed to cancel scheduling, Key is {}, Value is {}", e.getKey(), e.getValue()  );
    }
}
