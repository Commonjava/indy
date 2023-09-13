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
package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.conf.InternalFeatureConfig;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.cache.MavenMetadataCache;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class MetadataCacheManager
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @MavenMetadataCache
    private BasicCacheHandle<MetadataKey, MetadataInfo> metadataCache;

    @Inject
    private InternalFeatureConfig internalFeatureConfig;

    public MetadataCacheManager()
    {
    }

    public MetadataCacheManager( CacheHandle<MetadataKey, MetadataInfo> metadataCache, InternalFeatureConfig internalFeatureConfig )
    {
        this.metadataCache = metadataCache;
        this.internalFeatureConfig = internalFeatureConfig;
    }

    public void put( MetadataKey metadataKey, MetadataInfo metadataInfo )
    {
        if (internalFeatureConfig.isMavenMetadataCacheEnabled()) {
            metadataCache.put(metadataKey, metadataInfo);
        }
    }

    public MetadataInfo get( MetadataKey metadataKey )
    {
        if (internalFeatureConfig.isMavenMetadataCacheEnabled()) {
            return metadataCache.get(metadataKey);
        }
        return null;
    }

    public void remove( StoreKey storeKey, String path )
    {
        if (internalFeatureConfig.isMavenMetadataCacheEnabled()) {
            remove(new MetadataKey(storeKey, path));
        }
    }

    public void remove( MetadataKey metadataKey )
    {
        if (internalFeatureConfig.isMavenMetadataCacheEnabled()) {
            metadataCache.remove(metadataKey);
        }
    }
}
