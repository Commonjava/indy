/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.folo.data;

import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.subsys.infinispan.inject.qualifer.IndyCache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 */
public class FoloCacheProducer
{
    @Inject
    private CacheProducer cacheProducer;

    @FoloInprogressCache
    @Produces
    @ApplicationScoped
    public CacheHandle<TrackedContentEntry, TrackedContentEntry> inProgressFoloRecordCacheCfg()
    {
        return cacheProducer.getCache( "folo-in-progress", TrackedContentEntry.class, TrackedContentEntry.class );
    }

    @FoloSealedCache
    @Produces
    @ApplicationScoped
    public CacheHandle<TrackingKey, TrackedContent> sealedFoloRecordCacheCfg()
    {
        return cacheProducer.getCache( "folo-sealed", TrackingKey.class, TrackedContent.class );
    }
}
