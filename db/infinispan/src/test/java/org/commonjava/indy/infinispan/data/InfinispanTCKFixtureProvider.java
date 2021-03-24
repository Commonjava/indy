/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.infinispan.data;

import org.commonjava.indy.core.data.TCKFixtureProvider;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.commonjava.indy.infinispan.data.StoreDataCacheProducer.STORE_BY_PKG_CACHE;
import static org.commonjava.indy.infinispan.data.StoreDataCacheProducer.AFFECTED_BY_STORE_CACHE;
import static org.commonjava.indy.infinispan.data.StoreDataCacheProducer.STORE_BY_PKG_CACHE;
import static org.commonjava.indy.infinispan.data.StoreDataCacheProducer.STORE_DATA_CACHE;

public class InfinispanTCKFixtureProvider
        implements TCKFixtureProvider
{
    private InfinispanStoreDataManager dataManager;

    protected void init() throws IOException
    {
        DefaultCacheManager cacheManager = new DefaultCacheManager(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream( "infinispan-test.xml" ) );

        Cache<StoreKey, ArtifactStore> storeCache = cacheManager.getCache( STORE_DATA_CACHE, true );
        Cache<String, Map<StoreType, Set<StoreKey>>> storesByPkgCache = cacheManager.getCache( STORE_BY_PKG_CACHE, true );
        Cache<StoreKey, Set<StoreKey>> affected = cacheManager.getCache( AFFECTED_BY_STORE_CACHE, true );
        dataManager = new InfinispanStoreDataManager( storeCache, storesByPkgCache, affected );
    }

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }
}
