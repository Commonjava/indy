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
package org.commonjava.indy.core.ctl;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Set;

@ApplicationScoped
public class IspnCacheController
{
    public static final String ALL_CACHES = "all";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private CacheProducer cacheProducer;

    private EmbeddedCacheManager cacheManager;

    @PostConstruct
    private void setUp()
    {
        cacheManager = cacheProducer.getCacheManager();
    }

    public void clean( String name ) throws IndyWorkflowException
    {
        if ( ALL_CACHES.equals( name ) ) // clean all caches
        {
            Set<String> names = cacheManager.getCacheNames();
            names.forEach( ( n ) -> {
                if ( !isFoloCache( n ) ) // Prohibit clear of folo caches
                {
                   cacheManager.getCache( n ).clear();
                }
            } );
            return;
        }

        if ( isFoloCache( name ) )
        {
            throw new IndyWorkflowException( "Can not clean folo caches, name: " + name );
        }

        // clean named cache
        Cache<Object, Object> cache = cacheManager.getCache( name );
        if ( cache == null )
        {
            throw new IndyWorkflowException( "Cache not found, name: " + name );
        }
        cache.clear();
    }

    private boolean isFoloCache( String name )
    {
        return name != null && name.startsWith( "folo" );
    }
}
