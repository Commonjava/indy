/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.koji.conf.fixture;

import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.junit.rules.TemporaryFolder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.IOException;

import static org.junit.Assert.fail;

/**
 * Created by jdcasey on 6/7/16.
 */
@ApplicationScoped
public class TestProvider
{

    private StoreDataManager storeDataManager;

    private DefaultStorageProviderConfiguration storageProviderConfiguration;

    private NotFoundCache notFoundCache;

    private IndyObjectMapper objectMapper;

    private IndyConfiguration config;

    private TemporaryFolder temp = new TemporaryFolder();

    @PostConstruct
    public void setup()
    {
        try
        {
            temp.create();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail( "Failed to start temporary folder" );
        }

        storeDataManager = new MemoryStoreDataManager( true );
        try
        {
            storageProviderConfiguration = new DefaultStorageProviderConfiguration( temp.newFolder( "storage" ) );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail( "Failed to create temporary storage directory" );
        }

        notFoundCache = new MemoryNotFoundCache();

        objectMapper = new IndyObjectMapper( false );
        config = new DefaultIndyConfiguration();
    }

    @PreDestroy
    public void teardown()
    {
        temp.delete();
    }

    @Produces
    @Default
    public StoreDataManager getStoreDataManager()
    {
        return storeDataManager;
    }

    @Produces
    @Default
    public DefaultStorageProviderConfiguration getStorageProviderConfiguration()
    {
        return storageProviderConfiguration;
    }

    @Produces
    @Default
    public NotFoundCache getNotFoundCache()
    {
        return notFoundCache;
    }

    @Produces
    @Default
    public IndyObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    @Produces
    @Default
    public IndyConfiguration getConfig()
    {
        return config;
    }
}
