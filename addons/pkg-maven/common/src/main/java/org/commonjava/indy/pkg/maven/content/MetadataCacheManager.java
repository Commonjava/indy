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
package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.conf.InternalFeatureConfig;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.pkg.maven.content.cache.MavenMetadataCache;
import org.commonjava.indy.pkg.maven.content.cache.MavenMetadataKeyCache;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class MetadataCacheManager
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    @MavenMetadataCache
    private BasicCacheHandle<MetadataKey, MetadataInfo> metadataCache;

    @Inject
    private InternalFeatureConfig internalFeatureConfig;

    @Inject
    @MavenMetadataKeyCache
    private BasicCacheHandle<MetadataKey, MetadataKey> metadataKeyCache;

    private QueryFactory queryFactory;

    public MetadataCacheManager()
    {
    }

    @PostConstruct
    private void init()
    {
        if ( metadataKeyCache.getCache() instanceof RemoteCache )
        {
            this.queryFactory = org.infinispan.client.hotrod.Search.getQueryFactory(
                            (RemoteCache) metadataKeyCache.getCache() );
        }
        else
        {
            this.queryFactory = Search.getQueryFactory( (Cache) metadataKeyCache.getCache() );
        }
    }

    public MetadataCacheManager( CacheHandle<MetadataKey, MetadataInfo> metadataCache,
                                 CacheHandle<MetadataKey, MetadataKey> metadataKeyCache )
    {
        this.metadataCache = metadataCache;
        this.metadataKeyCache = metadataKeyCache;
        this.queryFactory = Search.getQueryFactory( metadataKeyCache.getCache() );
    }

    public void put( MetadataKey metadataKey, MetadataInfo metadataInfo )
    {
        if (internalFeatureConfig.isMavenMetadataCacheEnabled()) {
            metadataKeyCache.put(metadataKey, metadataKey);
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
            metadataKeyCache.remove(metadataKey);
            metadataCache.remove(metadataKey);
        }
    }

    /* 'removeAll' and 'getAllPaths' are only used for unit test. Do not use them! We may delete them in the future.
    metadataKeyCache is only for test too, and we may delete it as well. henry Mar 23, 2022 */

    @Deprecated
    public void removeAll( StoreKey key )
    {
        getMatches( key ).forEach( k -> remove( k ) );
    }

    @Deprecated
    public Set<String> getAllPaths( StoreKey key )
    {
        return getMatches( key ).stream().map( ( k ) -> k.getPath() ).collect( Collectors.toSet() );
    }

    private List<MetadataKey> getMatches( StoreKey key )
    {
        Query query = queryFactory.from( MetadataKey.class )
                                  .having( "storeKey.packageType" )
                                  .eq( key.getPackageType() )
                                  .and()
                                  .having( "storeKey.type" )
                                  .eq( key.getType().toString() )
                                  .and()
                                  .having( "storeKey.name" )
                                  .eq( key.getName() )
                                  .build();
        List<MetadataKey> matches = query.list();

        logger.debug( "Query metadataKeyCache for storeKey: {}, size: {}", key, matches.size() );
        return matches;
    }

}
