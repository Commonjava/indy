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
package org.commonjava.indy.subsys.template.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;
import org.commonjava.cdi.util.weft.config.WeftConfig;
import org.commonjava.indy.action.UserLifecycleManager;
import org.commonjava.indy.action.fixture.AlternativeUserLifecycleManager;
import org.commonjava.indy.content.IndyPathGenerator;
import org.commonjava.indy.data.NoOpStoreEventDispatcher;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.data.StoreValidator;
import org.commonjava.indy.db.common.inject.Standalone;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.config.TransportManagerConfig;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.io.TransferDecoratorManager;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.proxy.NoOpProxySitesCache;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.spi.proxy.ProxySitesCache;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalHttpConfiguration;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalProxyConfig;
import org.junit.rules.TemporaryFolder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 11/17/16.
 */
@ApplicationScoped
public class TestProvider
{
    @Inject
    private IndyPathGenerator indyPathGenerator;

    private NotFoundCache nfc;

    private ProxySitesCache proxySitesCache;

    private StoreDataManager storeDataManager;

    private ObjectMapper objectMapper;

    private CacheProvider cacheProvider;

    private FileEventManager fileEventManager;

    private NoOpTransferDecorator transferDecorator;

    private TransportManagerConfig transportManagerConfig;

    private TemporaryFolder temp;

    private WeftConfig weftConfig;

    private GlobalHttpConfiguration globalHttpConfiguration;

    private UserLifecycleManager userLifecycleManager;

    private StoreEventDispatcher eventDispatcher;

    private StoreValidator storeValidator;

    @PostConstruct
    public void setup()
    {
        storeDataManager = new MemoryStoreDataManager( true );
        nfc = new MemoryNotFoundCache();
        proxySitesCache = new NoOpProxySitesCache();
        objectMapper = new IndyObjectMapper( false );
        fileEventManager = new NoOpFileEventManager();
        transferDecorator = new NoOpTransferDecorator();
        transportManagerConfig = new TransportManagerConfig();
        weftConfig = new DefaultWeftConfig();
        globalHttpConfiguration = new GlobalHttpConfiguration();
        userLifecycleManager = new AlternativeUserLifecycleManager();
        eventDispatcher = new NoOpStoreEventDispatcher();
        storeValidator = artifactStore -> null;

        temp = new TemporaryFolder();
        try
        {
            temp.create();

            cacheProvider =
                    new FileCacheProvider( temp.newFolder( "storage" ), indyPathGenerator, fileEventManager,
                                           new TransferDecoratorManager( transferDecorator ) );
        }
        catch ( IOException e )
        {
            fail( "Cannot initialize temporary directory structure" );
            temp.delete();
        }
    }

    @PreDestroy
    public void teardown()
    {
        if ( temp != null )
        {
            temp.delete();
        }
    }

    @Produces
    public NotFoundCache getNfc()
    {
        return nfc;
    }

    @Produces
    public ProxySitesCache getProxySitesCache()
    {
        return proxySitesCache;
    }

    @Produces
    @Standalone
    @Default
    public StoreDataManager getStoreDataManager()
    {
        return storeDataManager;
    }

    @Produces
    public StoreValidator getStoreValidator(){
        return storeValidator;
    }

    @Produces
    public StoreEventDispatcher getEventDispatcher(){
        return eventDispatcher;
    }

    @Produces
    public ObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    @Produces
    public CacheProvider getCacheProvider()
    {
        return cacheProvider;
    }

    @Produces
    public FileEventManager getFileEventManager()
    {
        return fileEventManager;
    }

    @Produces
    public NoOpTransferDecorator getTransferDecorator()
    {
        return transferDecorator;
    }

    @Produces
    public TransportManagerConfig getTransportManagerConfig()
    {
        return transportManagerConfig;
    }

    @Produces
    public WeftConfig getWeftConfig()
    {
        return weftConfig;
    }

    @Produces
    public GlobalHttpConfiguration getGlobalHttpConfiguration()
    {
        return globalHttpConfiguration;
    }

    @Produces
    public UserLifecycleManager getUserLifecycleManager()
    {
        return userLifecycleManager;
    }

//    @Produces
//    public TracerConfiguration getTracerConfiguration()
//    {
//        return null;
//    }
//
//    @Produces
//    public GoldenSignalsMetricSet getGoldenSignalsMetricSet()
//    {
//        return null;
//    }
}
