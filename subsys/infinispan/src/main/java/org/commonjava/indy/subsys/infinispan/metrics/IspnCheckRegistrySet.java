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
package org.commonjava.indy.subsys.infinispan.metrics;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class IspnCheckRegistrySet
{
    private static Logger logger = LoggerFactory.getLogger( IspnCheckRegistrySet.class );

    public static final String INDY_METRIC_ISPN = "indy.ispn";

    private static final String SIZE = "size";

    private static final String CURRENT_NUMBER_OF_ENTRIES = "CurrentNumberOfEntries";

    private static final String NUMBER_OF_ENTRIES_ADDED = "NumberOfEntriesAdded";

    private static final String CURRENT_NUMBER_OF_ENTRIES_IN_MEMORY = "CurrentNumberOfEntriesInMemory";

    private static final String TOTAL_NUMBER_OF_ENTRIES = "TotalNumberOfEntries";

    private static final String HITS = "Hits";

    private static final String MISSES = "Misses";

    private static final String RETRIEVALS = "Retrievals";

    private static final String EVICTIONS = "Evictions";

    private static final String REMOVALS = "Removals";

    private static final String TOTAL_HITS = "TotalHits";

    private static final String TOTAL_MISSES = "TotalMisses";

    private static final String TOTAL_RETRIEVALS = "TotalRetrievals";

    private static final String TOTAL_EVICTIONS = "TotalEvictions";

    private static final String TOTAL_REMOVALS = "TotalRemovals";

    private static final String OFF_HEAP_MEMORY_USED = "OffHeapMemoryUsed";

    private static final String DATA_MEMORY_USED = "DataMemoryUsed";

    private static final String AVG_READ_TIME = "AvgReadTime";

    private static final String AVG_WRITE_TIME = "AvgWriteTime";

    private static final String AVG_REMOVE_TIME = "AvgRemoveTime";

    private EmbeddedCacheManager cacheManager;

    private List<String> ispnGauges;

    public IspnCheckRegistrySet( EmbeddedCacheManager cacheManager, List<String> ispnGauges )
    {
        this.cacheManager = cacheManager;
        this.ispnGauges = ispnGauges;
    }

}
