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
package org.commonjava.indy.content.index;

import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.indy.subsys.infinispan.inject.qualifer.IndyCache;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.cfg.SearchMapping;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.spi.SearchManagerImplementor;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.lang.annotation.ElementType;
import java.util.Properties;

public class ContentIndexCacheProducer
{
    @Inject
    private CacheProducer cacheProducer;

    @PostConstruct
    public void initIndexing(){
        registerIndexableEntities();
        regesterTransformer();
    }

    private void registerIndexableEntities(){
        final SearchMapping indexMapping = new SearchMapping();
        indexMapping.entity( IndexedStorePath.class ).indexed().indexName( "indexedStorePath" )
                    .property( "storeType", ElementType.METHOD ).field()
                        .name( "storeType" ).store( Store.YES ).analyze( Analyze.NO )
                    .property( "storeName", ElementType.METHOD ).field()
                        .name( "storeName" ).store( Store.YES ).analyze( Analyze.NO )
                    .property( "originStoreType", ElementType.METHOD ).field()
                        .name( "originStoreType" ).store( Store.YES ).analyze( Analyze.NO )
                    .property( "originStoreName", ElementType.METHOD ).field()
                        .name( "originStoreName" ).store( Store.YES ).analyze( Analyze.NO )
                    .property( "path", ElementType.METHOD ).field()
                        .name( "path" ).store( Store.YES ).analyze( Analyze.NO );

        Properties properties = new Properties();
        properties.put( Environment.MODEL_MAPPING, indexMapping );

        final Configuration contentIndex = cacheProducer.getCacheConfiguration( "content-index" );

        if ( contentIndex != null )
        {
            final Configuration indexingConfig = new ConfigurationBuilder().read( contentIndex )
                                                                           .indexing()
                                                                           .withProperties( properties )
                                                                           .index( Index.LOCAL )
                                                                           .build();
            cacheProducer.setCacheConfiguration( "content-index", indexingConfig );
        }
    }

    private void regesterTransformer(){
        final CacheHandle<IndexedStorePath, IndexedStorePath> handler =
                cacheProducer.getCache( "content-index", IndexedStorePath.class, IndexedStorePath.class );
        final SearchManagerImplementor searchManager =
                (SearchManagerImplementor) org.infinispan.query.Search.getSearchManager( handler.getCache() );
        searchManager.registerKeyTransformer( IndexedStorePath.class, IndexedStorePathTransformer.class );
    }

    @ContentIndexCache
    @Produces
    @ApplicationScoped
    public CacheHandle<IndexedStorePath, IndexedStorePath> contentIndexCacheCfg()
    {
        return cacheProducer.getCache( "content-index", IndexedStorePath.class, IndexedStorePath.class );
    }
}
