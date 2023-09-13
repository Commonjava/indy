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
package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * @deprecated The store management functions has been extracted into Repository Service, which is maintained in "ServiceStoreDataManager"
 */
@Deprecated
public class StoreDataCacheProducer
{

    public static final String REMOTE_KOJI_STORE = "remote-koji-stores";

    @Inject
    private CacheProducer cacheProducer;

    @RemoteKojiStoreDataCache
    @Produces
    @ApplicationScoped
    public CacheHandle<StoreKey, ArtifactStore> getRemoteKojiStoreDataCache()
    {
        return cacheProducer.getCache(REMOTE_KOJI_STORE);
    }

}
