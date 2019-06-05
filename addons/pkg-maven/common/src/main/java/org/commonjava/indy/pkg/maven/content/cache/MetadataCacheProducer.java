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
package org.commonjava.indy.pkg.maven.content.cache;

import org.commonjava.indy.pkg.maven.content.MetadataKey;
import org.commonjava.indy.pkg.maven.content.MetadataInfo;
import org.commonjava.indy.pkg.maven.content.MetadataKeyTransformer;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.query.Search;
import org.infinispan.query.spi.SearchManagerImplementor;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class MetadataCacheProducer
{
    private static final String METADATA_KEY_CACHE = "maven-metadata-key-cache";

    private static final String METADATA_CACHE = "maven-metadata-cache";

    @Inject
    private CacheProducer cacheProducer;

    @MavenMetadataCache
    @Produces
    @ApplicationScoped
    public CacheHandle<MetadataKey, MetadataInfo> mavenMetadataCacheCfg()
    {
        return cacheProducer.getCache( METADATA_CACHE );
    }

    @MavenMetadataKeyCache
    @Produces
    @ApplicationScoped
    public CacheHandle<MetadataKey, MetadataKey> mavenMetadataKeyCacheCfg()
    {
        return cacheProducer.getCache( METADATA_KEY_CACHE );
    }

    @PostConstruct
    public void initIndexing()
    {
        registerTransformer();
    }

    private void registerTransformer()
    {
        final CacheHandle<MetadataKey, MetadataKey> handler = cacheProducer.getCache( METADATA_KEY_CACHE );
        handler.executeCache( cache -> {
            SearchManagerImplementor searchManager = (SearchManagerImplementor) Search.getSearchManager( cache );
            searchManager.registerKeyTransformer( MetadataKey.class, MetadataKeyTransformer.class );
            return null;
        } );
    }
}
