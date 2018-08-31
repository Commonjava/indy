/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Map;

/**
 * This ISPN cache producer is used to generate {@link ScheduleCache}. This type of cache is used as a job control center
 * to control the content expiration scheduling.
 */
public class ScheduleCacheProducer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CacheProducer cacheProducer;

    private static final String SCHEDULE_EXPIRE = "schedule-expire-cache";

    @ScheduleCache
    @Produces
    @ApplicationScoped
    public CacheHandle<ScheduleKey, Map> versionMetadataCache()
    {
        return cacheProducer.getCache( SCHEDULE_EXPIRE );
    }

}
