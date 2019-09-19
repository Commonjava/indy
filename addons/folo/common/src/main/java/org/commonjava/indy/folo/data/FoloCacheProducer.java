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
package org.commonjava.indy.folo.data;

import org.commonjava.indy.folo.data.idxmodel.StoreKeyFieldBridge;
import org.commonjava.indy.folo.data.idxmodel.TrackedContentEntryTransformer;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.subsys.datafile.conf.DataFileConfiguration;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.cfg.SearchMapping;
import org.infinispan.query.Search;
import org.infinispan.query.spi.SearchManagerImplementor;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.lang.annotation.ElementType;

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

    @Inject
    private DataFileConfiguration dataConfig;

    @PostConstruct
    public void initIndexing()
    {
        registerTransformer();
    }

    @Factory
    public SearchMapping getSearchMapping()
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

        return entryMapping;
    }

    private void registerTransformer(){
        final CacheHandle<TrackedContentEntry, TrackedContentEntry> handler =
                        cacheProducer.getCache( IN_PROGRESS_NAME );

        handler.executeCache( cache->{
            SearchManagerImplementor searchManager = (SearchManagerImplementor) Search.getSearchManager( cache );

            searchManager.registerKeyTransformer( TrackedContentEntry.class, TrackedContentEntryTransformer.class );

            return null;
        } );
    }

    @FoloInprogressCache
    @Produces
    @ApplicationScoped
    public CacheHandle<TrackedContentEntry, TrackedContentEntry> inProgressFoloRecordCacheCfg()
    {
        return cacheProducer.getCache( IN_PROGRESS_NAME );
    }

    @FoloSealedCache
    @Produces
    @ApplicationScoped
    public CacheHandle<TrackingKey, TrackedContent> sealedFoloRecordCacheCfg()
    {
        return cacheProducer.getCache( SEALED_NAME );
    }
}
