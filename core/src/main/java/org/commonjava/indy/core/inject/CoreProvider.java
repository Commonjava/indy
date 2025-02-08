/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.inject;

import com.fasterxml.jackson.databind.Module;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.model.core.io.ModuleSet;
import org.commonjava.indy.subsys.cassandra.CassandraClient;
import org.commonjava.indy.subsys.infinispan.BasicCacheHandle;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.commonjava.maven.galley.proxy.NoOpProxySitesCache;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.spi.proxy.ProxySitesCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.commonjava.indy.conf.DefaultIndyConfiguration.CASSANDRA_NFC_PROVIDER;

@ApplicationScoped
public class CoreProvider
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyConfiguration indyConfiguration;

    @Inject
    private CassandraClient cassandraClient;

    @Inject
    private CacheProducer cacheProducer;

    @Inject
    @NfcCache
    private BasicCacheHandle<String, NfcConcreteResourceWrapper> nfcCache;

    @Inject
    private Instance<Module> objectMapperModules;

    @Inject
    private Instance<ModuleSet> objectMapperModuleSets;

    private IndyObjectMapper objectMapper;

    public CoreProvider()
    {
    }

    @PostConstruct
    public void init()
    {
        this.objectMapper = new IndyObjectMapper( objectMapperModules, objectMapperModuleSets );

        String nfcProvider = indyConfiguration.getNfcProvider();
        logger.info( "Apply nfc provider: {}", nfcProvider );
        if ( CASSANDRA_NFC_PROVIDER.equals( nfcProvider ) )
        {
            notFoundCache = new CassandraNotFoundCache( indyConfiguration, cacheProducer,
                                                        cassandraClient );
        }
        else
        {
            notFoundCache = new IspnNotFoundCache( indyConfiguration, nfcCache ); // default
        }

    }

    @Produces
    @Default
    public IndyObjectMapper getIndyObjectMapper()
    {
        return objectMapper;
    }


    private volatile NotFoundCache notFoundCache;

    @Produces
    @Default
    public NotFoundCache getNotFoundCache() { return notFoundCache; }

    @Produces
    public ProxySitesCache getPCache()
    {
        return new NoOpProxySitesCache();
    }
}
