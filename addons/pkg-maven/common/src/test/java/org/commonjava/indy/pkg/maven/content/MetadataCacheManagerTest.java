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
package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MetadataCacheManagerTest
{
    private MetadataCacheManager metadataCacheManager;

    private CacheProducer cacheProducer;

    @Before
    public void setup() throws Exception
    {
        DefaultCacheManager cacheManager =
                        new DefaultCacheManager( new ConfigurationBuilder().simpleCache( true ).build() );
        cacheProducer = new CacheProducer( null, cacheManager, null );
        CacheHandle<MetadataKey, MetadataKey> metadataKeyCache = cacheProducer.getCache( "maven-metadata-key-cache" );
        CacheHandle<MetadataKey, MetadataInfo> metadataCache = cacheProducer.getCache( "maven-metadata-cache" );
        metadataCacheManager = new MetadataCacheManager( metadataCache, metadataKeyCache );
    }

    @Test
    public void query() throws Exception
    {
        final MetadataInfo info = new MetadataInfo( null );
        StoreKey hosted = StoreKey.fromString( "maven:hosted:test" );
        StoreKey remote = StoreKey.fromString( "maven:remote:test" );

        Set<String> paths = new HashSet<>();
        for ( int i = 0; i < 20; i++ )
        {
            paths.add( "path/to/" + i );
        }

        paths.forEach( p -> {
            metadataCacheManager.put( new MetadataKey( hosted, p ), info );
            metadataCacheManager.put( new MetadataKey( remote, p ), info );
        } );

        String somePath = "path/to/3";

        MetadataInfo ret = metadataCacheManager.get( new MetadataKey( hosted, somePath ) );
        assertNotNull( ret );

        Set<String> allPaths = metadataCacheManager.getAllPaths( hosted );
        assertTrue( allPaths.size() == 20 );

        metadataCacheManager.removeAll( hosted );
        allPaths = metadataCacheManager.getAllPaths( hosted );
        assertTrue( allPaths.size() == 0 );

        ret = metadataCacheManager.get( new MetadataKey( hosted, somePath ) );
        assertNull( ret );

        ret = metadataCacheManager.get( new MetadataKey( remote, somePath ) );
        assertNotNull( ret );
    }

    @After
    public void tearDown() throws Exception
    {
        cacheProducer.stop();
    }

}
