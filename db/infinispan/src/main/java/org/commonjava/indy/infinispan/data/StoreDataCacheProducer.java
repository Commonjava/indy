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
package org.commonjava.indy.infinispan.data;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

public class StoreDataCacheProducer
{
    public static final String STORE_DATA_CACHE = "store-data-v2";

    public static final String STORE_BY_PKG_CACHE = "store-by-package";

    public static final String AFFECTED_BY_STORE_CACHE = "affected-by-stores";

    public static final String STORE_BY_PKG_CACHE = "store-by-package";

    @Inject
    private CacheProducer cacheProducer;

    @StoreDataCache
    @Produces
    @ApplicationScoped
    public CacheHandle<StoreKey, ArtifactStore> getStoreDataCache()
    {
        return cacheProducer.getCache( STORE_DATA_CACHE );
    }

//    @StoreDataCache
//    @Produces
//    @ApplicationScoped
//    public CacheHandle<StoreKey, String> getStoreDataCache()
//    {
//        return cacheProducer.getCache( STORE_DATA_CACHE );
//    }

    @StoreByPkgCache
    @Produces
    @ApplicationScoped
    public CacheHandle<String, Map<StoreType, Set<StoreKey>>> getStoreByPkgCache()
    {
        return cacheProducer.getCache( STORE_BY_PKG_CACHE );
    }

    @AffectedByStoreCache
    @Produces
    @ApplicationScoped
    public CacheHandle<StoreKey, Set<StoreKey>> getAffectedByStores()
    {
        return cacheProducer.getCache( AFFECTED_BY_STORE_CACHE );
    }

    @StoreByPkgCache
    @Produces
    @ApplicationScoped
    public CacheHandle<String, Map<StoreType, Set<StoreKey>>> getStoreByPkgCache()
    {
        return cacheProducer.getCache( STORE_BY_PKG_CACHE );
    }

}