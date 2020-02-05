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
package org.commonjava.indy.core.ctl;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@ApplicationScoped
public class IspnCacheController
{
    public static final String ALL_CACHES = "all";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CacheProducer cacheProducer;

    @Inject
    private IndyObjectMapper mapper;

    private EmbeddedCacheManager cacheManager;

    private Map<String, String> string2keyMapper;

    @PostConstruct
    private void setUp()
    {
        cacheManager = cacheProducer.getCacheManager();
        string2keyMapper = new HashMap<>();
        string2keyMapper.put( "content-index", "org.commonjava.indy.content.index.ISPFieldStringKey2StringMapper" );
        string2keyMapper.put( "default", "org.commonjava.indy.pkg.maven.content.StoreKey2StringMapper" );
    }

    // only work for some caches for debugging
    public String export( String cacheName, String key ) throws Exception
    {
        Cache<Object, Object> cache = cacheManager.getCache( cacheName );
        if ( cache == null )
        {
            throw new IndyWorkflowException( "Cache not found, name: " + cacheName );
        }
        if ( isNotBlank( key ) )
        {
            if ( key.startsWith( "/" ) )
            {
                key = key.substring( 1 );
            }
            String stringMapperClass = string2keyMapper.get( cacheName );
            if ( stringMapperClass == null )
            {
                stringMapperClass = string2keyMapper.get( "default" );
            }
            TwoWayKey2StringMapper stringMapper =
                            (TwoWayKey2StringMapper) Class.forName( stringMapperClass ).newInstance();
            return mapper.writeValueAsString( cache.get( stringMapper.getKeyMapping( key ) ) );
        }
        else
        {
            return mapper.writeValueAsString( cache.entrySet() );
        }
    }

}
