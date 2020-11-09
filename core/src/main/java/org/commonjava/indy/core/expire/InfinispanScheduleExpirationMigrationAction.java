/**
 * Copyright (C) 2020 Red Hat, Inc.
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
package org.commonjava.indy.core.expire;

import org.commonjava.indy.action.MigrationAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Named( "infinispan-schedule-expiration-migration" )
public class InfinispanScheduleExpirationMigrationAction
        implements MigrationAction
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String SCHEDULE_CACHE_V1 = "schedule-expire-cache";

    @Inject
    private DataFileManager dataFileManager;

    @Inject
    private CacheProducer cacheProducer;

    @Inject
    private CacheHandle<ScheduleKey, ScheduleValue> scheduleCacheV2;

    @Inject
    private IndyObjectMapper objectMapper;

    protected InfinispanScheduleExpirationMigrationAction()
    {
    }

    @Override
    public boolean migrate()
    {
        final Set<ScheduleKey> result = Collections.synchronizedSet( new HashSet<>() );
        if ( cacheProducer != null )
        {
            EmbeddedCacheManager cacheManager = cacheProducer.getCacheManager();
            if ( cacheManager.cacheExists( SCHEDULE_CACHE_V1 ) )
            {
                logger.info( "Migrating from legacy schedule expiration cache: {}", SCHEDULE_CACHE_V1 );
                cacheProducer.getCache( SCHEDULE_CACHE_V1 ).executeCache( c -> {
                    c.forEach( ( key, value ) -> {
                        if ( key instanceof ScheduleKey && value instanceof Map )
                        {
                            logger.info( "Migrating from legacy cache: {}", key );
                            scheduleCacheV2.put( (ScheduleKey) key,
                                                 new ScheduleValue( (ScheduleKey) key, (Map) value ) );
                            result.add( (ScheduleKey) key );
                        }
                    } );
                    c.clearAsync();
                    return null;
                } );
            }
        }
        return !result.isEmpty();
    }

    @Override
    public int getMigrationPriority()
    {
        return 99;
    }

    @Override
    public String getId()
    {
        return "Legacy ISPN schedule-expiration cache data migrator";
    }
}
