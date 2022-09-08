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
package org.commonjava.indy.cassandra.data;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.core.conf.IndyStoreManagerConfig;
import org.commonjava.indy.core.data.TCKFixtureProvider;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.cassandra.config.CassandraConfig;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class CassandraTCKFixtureProvider
        implements TCKFixtureProvider
{
    private static CassandraStoreDataManager dataManager;

    private static CassandraClient client;

    private CacheProducer cacheProducer;

    protected void init()
            throws Exception
    {
        EmbeddedCassandraServerHelper.startEmbeddedCassandra();

        CassandraConfig config = new CassandraConfig();
        config.setEnabled( true );
        config.setCassandraHost( "localhost" );
        config.setCassandraPort( 9142 );

        client = new CassandraClient( config );
        IndyStoreManagerConfig storeConfig = new IndyStoreManagerConfig( "noncontent", 1);

        DefaultIndyConfiguration indyConfig = new DefaultIndyConfiguration();
        indyConfig.setKeyspaceReplicas( 1 );

        client = new CassandraClient( config );

        DefaultCacheManager cacheManager =
                new DefaultCacheManager( new ConfigurationBuilder().simpleCache( true ).build() );
        cacheProducer = new CacheProducer( null, cacheManager, null );
        CassandraStoreQuery storeQuery = new CassandraStoreQuery( client, storeConfig, indyConfig );
        dataManager = new CassandraStoreDataManager( storeQuery, new IndyObjectMapper( true ), cacheProducer );
    }

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }

    protected void clean(){
        EmbeddedCassandraServerHelper.getSession();
        EmbeddedCassandraServerHelper.cleanDataEmbeddedCassandra("noncontent");
    }

    protected void destroy()
    {
        client.close();
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }
}
