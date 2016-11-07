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

import org.commonjava.indy.folo.data.idxmodel.StoreKeyFieldBridge;
import org.commonjava.indy.folo.data.idxmodel.TrackedContentEntryTransformer;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.query.spi.SearchManagerImplementor;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Properties;

/**
 * This ISPN cache producer has some self-defined indexing logic. This directly uses ISPN/hibernate search api
 * to configure the indexable keys used in folo-sealed cache to decouple the folo/model-java dependency on ISPN
 * libraries.
 */
public class FoloCacheProducer
{

    private static final String SEALED_NAME = "folo-sealed";

    private static final String IN_PROGRESS_NAME = "folo-in-progress";

    @Inject
    private CacheProducer cacheProducer;

    @PostConstruct
    public void initIndexing()
    {
        registerIndexableEntities();
        registerTransformer();
    }

    private void registerIndexableEntities()
    {
        final SearchMapping entryMapping = new SearchMapping();
        entryMapping.entity( TrackedContentEntry.class ).indexed()
                    .property( "storeKey", ElementType.METHOD ).field().bridge( StoreKeyFieldBridge.class )
                    .property( "accessChannel", ElementType.METHOD ).field()
                    .property( "path", ElementType.METHOD ).field()
                    .property( "originUrl", ElementType.METHOD ).field()
                    .property( "effect", ElementType.METHOD ).field()
                    .property( "md5", ElementType.METHOD ).field()
                    .property( "sha256", ElementType.METHOD ).field()
                    .property( "sha1", ElementType.METHOD ).field()
                    .property( "size", ElementType.METHOD ).field()
                    .property( "index", ElementType.METHOD ).field().analyze( Analyze.NO )
                    .property( "trackingKey", ElementType.METHOD )
                    .indexEmbedded().entity( TrackingKey.class ).indexed()
                    .property( "id", ElementType.METHOD ).field().analyze( Analyze.NO );

        Properties properties = new Properties();
        properties.put( Environment.MODEL_MAPPING, entryMapping);

        Configuration sealedConfig=
                cacheProducer.getCacheConfiguration( SEALED_NAME );

        if ( sealedConfig == null )
        {
            sealedConfig = cacheProducer.getDefaultCacheConfiguration();
        }

        if ( sealedConfig != null )
        {
            final Configuration indexingConfig =
                    new ConfigurationBuilder().read( sealedConfig ).indexing().withProperties( properties ).index(
                            Index.LOCAL ).build();

            cacheProducer.setCacheConfiguration( SEALED_NAME, indexingConfig );
        }
    }

    private void registerTransformer(){
        final CacheHandle<TrackingKey, TrackedContent> handler =
                cacheProducer.getCache( SEALED_NAME, TrackingKey.class, TrackedContent.class );

        SearchManagerImplementor searchManager = handler.execute( cache -> (SearchManagerImplementor) Search.getSearchManager( cache ) );

        searchManager.registerKeyTransformer( TrackedContentEntry.class, TrackedContentEntryTransformer.class );
    }

    @FoloInprogressCache
    @Produces
    @ApplicationScoped
    public CacheHandle<TrackedContentEntry, TrackedContentEntry> inProgressFoloRecordCacheCfg()
    {
        return cacheProducer.getCache( IN_PROGRESS_NAME, TrackedContentEntry.class, TrackedContentEntry.class );
    }

    @FoloSealedCache
    @Produces
    @ApplicationScoped
    public CacheHandle<TrackingKey, TrackedContent> sealedFoloRecordCacheCfg()
    {
        return cacheProducer.getCache( SEALED_NAME, TrackingKey.class, TrackedContent.class );
    }
}
