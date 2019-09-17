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
package org.commonjava.indy.subsys.template.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.cdi.util.weft.config.DefaultWeftConfig;
import org.commonjava.cdi.util.weft.config.WeftConfig;
import org.commonjava.indy.action.UserLifecycleManager;
import org.commonjava.indy.action.fixture.AlternativeUserLifecycleManager;
import org.commonjava.indy.content.IndyPathGenerator;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.config.TransportManagerConfig;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.io.TransferDecoratorManager;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.transport.htcli.conf.GlobalHttpConfiguration;
import org.junit.rules.TemporaryFolder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.IOException;

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

    @PostConstruct
    public void setup()
    {
        storeDataManager = new MemoryStoreDataManager( true );
        nfc = new MemoryNotFoundCache();
        objectMapper = new IndyObjectMapper( false );
        fileEventManager = new NoOpFileEventManager();
        transferDecorator = new NoOpTransferDecorator();
        transportManagerConfig = new TransportManagerConfig();
        weftConfig = new DefaultWeftConfig();
        globalHttpConfiguration = new GlobalHttpConfiguration();
        userLifecycleManager = new AlternativeUserLifecycleManager();

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
    public StoreDataManager getStoreDataManager()
    {
        return storeDataManager;
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

}
