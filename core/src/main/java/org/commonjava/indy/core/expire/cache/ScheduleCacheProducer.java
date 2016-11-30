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

import org.commonjava.indy.core.expire.ScheduleKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.notifications.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This ISPN cache producer is used to generate {@link ScheduleCache}. This type of cache is used as a job control center
 * to control the content expiration scheduling.
 */
@Listener
public class ScheduleCacheProducer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CacheProducer cacheProducer;

    private static final String SCHEDULE_EXPIRE = "schedule-expire-cache";

    @PostConstruct
    public void initExpireConfig()
    {
        //TODO: this wakeUpInterval is used to trigger the purge threads of ISPN to let it purge the cache, which will
        //      trigger the cache expire event to happen. Did not find a way to let the expire event automatically happen
        //      without this. See http://infinispan.org/docs/stable/faqs/faqs.html#eviction_and_expiration_questions for more
        final Configuration c = new ConfigurationBuilder().expiration().wakeUpInterval( 1, TimeUnit.SECONDS ).build();
        cacheProducer.setCacheConfiguration( SCHEDULE_EXPIRE, c );
    }

    @ScheduleCache
    @Produces
    @ApplicationScoped
    public CacheHandle<ScheduleKey, Map> versionMetadataCache()
    {
        return cacheProducer.getCache( SCHEDULE_EXPIRE, ScheduleKey.class, Map.class );
    }

}
