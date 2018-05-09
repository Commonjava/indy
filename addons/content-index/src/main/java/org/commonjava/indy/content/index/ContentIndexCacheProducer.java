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
package org.commonjava.indy.content.index;

import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

public class ContentIndexCacheProducer
{
    @Inject
    private CacheProducer cacheProducer;

//    @Inject
//    private DataFileConfiguration config;

//    @PostConstruct
//    public void initIndexing(){
//        registerTransformer();
//    }

//    @Factory
//    public SearchMapping getSearchMapping()
//    {
//        final SearchMapping mapping = new SearchMapping();
//        mapping.entity( IndexedStorePath.class ).indexed().indexName( "indexedStorePath" )
//               .property( "storeType", ElementType.METHOD ).field()
//               .name( "storeType" ).store( Store.YES ).analyze( Analyze.NO )
//               .property( "storeName", ElementType.METHOD ).field()
//               .name( "storeName" ).store( Store.YES ).analyze( Analyze.NO )
//               .property( "originStoreType", ElementType.METHOD ).field()
//               .name( "originStoreType" ).store( Store.YES ).analyze( Analyze.NO )
//               .property( "originStoreName", ElementType.METHOD ).field()
//               .name( "originStoreName" ).store( Store.YES ).analyze( Analyze.NO )
//               .property( "path", ElementType.METHOD ).field()
//               .name( "path" ).store( Store.YES ).analyze( Analyze.NO );
//
//        return mapping;
//    }

//    private void registerTransformer(){
//        final CacheHandle<IndexedStorePath, IndexedStorePath> handler =
//                cacheProducer.getCache( "content-index", IndexedStorePath.class, IndexedStorePath.class );
//
//        handler.execute( cache->{
//            final SearchManagerImplementor searchManager =
//                    (SearchManagerImplementor) org.infinispan.query.Search.getSearchManager( cache );
//
//            searchManager.registerKeyTransformer( IndexedStorePath.class, IndexedStorePathTransformer.class );
//            return null;
//        } );
//    }

    @ContentIndexCache
    @Produces
    @ApplicationScoped
    public CacheHandle<IndexedStorePath, IndexedStorePath> contentIndexCacheCfg()
    {
        return cacheProducer.getCache( "content-index", IndexedStorePath.class, IndexedStorePath.class );
    }
}
