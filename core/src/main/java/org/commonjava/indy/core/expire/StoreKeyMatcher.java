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
package org.commonjava.indy.core.expire;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheKeyMatcher;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A key matcher which is used to match the cache key with store key.
 *
 */
public class StoreKeyMatcher
        implements CacheKeyMatcher<ScheduleKey>
{

    //TODO: will have a thought to replace this type of matcher with a ISPN query API in the future to get better performance.

    private final String groupName;

    public StoreKeyMatcher( final StoreKey key, final String eventType )
    {
        this.groupName = ScheduleManagerUtils.groupName( key, eventType );
    }

    @Override
    public Set<ScheduleKey> matches( CacheHandle<ScheduleKey, ?> cacheHandle )
    {
        QueryFactory queryFactory = Search.getQueryFactory( cacheHandle.getCache() );
        Query q = queryFactory.from( ScheduleValue.class ).having( "key.groupName" ).eq( groupName ).build();
        List<ScheduleValue> list = q.list();
        return list.stream().map( ScheduleValue::getKey ).collect( Collectors.toSet());
//        return cacheHandle.execute( BasicCache::keySet )
//                          .stream()
//                          .filter( key -> key != null && key.exists() && key.groupName().equals( groupName ) )
//                          .collect( Collectors.toSet() );
    }
}
