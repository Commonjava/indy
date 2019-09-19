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
package org.commonjava.indy.promote.fixture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.commonjava.indy.core.change.event.IndyFileEventManager;
import org.commonjava.indy.content.IndyPathGenerator;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.config.TransportManagerConfig;
import org.commonjava.maven.galley.internal.TransferManagerImpl;
import org.commonjava.maven.galley.internal.xfer.DownloadHandler;
import org.commonjava.maven.galley.internal.xfer.ExistenceHandler;
import org.commonjava.maven.galley.internal.xfer.ListingHandler;
import org.commonjava.maven.galley.internal.xfer.UploadHandler;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.io.SpecialPathManagerImpl;
import org.commonjava.maven.galley.io.TransferDecoratorManager;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.TransportManagerImpl;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;

public class GalleyFixture
{

    private final TransferManager transfers;

    private final TransportManager transports;

    private final CacheProvider cache;

    private final FileEventManager events;

    private final TransferDecoratorManager decorator;

    private final ExecutorService executor;

    private final NotFoundCache nfc;

    private final ExecutorService batchExecutor;

    private final SpecialPathManagerImpl specialPathManager;

    public GalleyFixture( final File repoRoot )
    {
        MemoryPasswordManager passwordManager = new MemoryPasswordManager();

        transports = new TransportManagerImpl( new HttpClientTransport( new HttpImpl(passwordManager) ) );

        events = new IndyFileEventManager();
        decorator = new TransferDecoratorManager( new NoOpTransferDecorator() );
        cache = new FileCacheProvider( repoRoot, new IndyPathGenerator(), events, decorator );
        executor = Executors.newFixedThreadPool( 2 );
        batchExecutor = Executors.newFixedThreadPool( 2 );
        nfc = new MemoryNotFoundCache();
        specialPathManager = new SpecialPathManagerImpl();

        TransportManagerConfig transportManagerConfig = new TransportManagerConfig();

        final DownloadHandler dh = new DownloadHandler( nfc, transportManagerConfig, executor );
        final UploadHandler uh = new UploadHandler( nfc, transportManagerConfig, executor );
        final ListingHandler lh = new ListingHandler( nfc );
        final ExistenceHandler eh = new ExistenceHandler( nfc );

        transfers = new TransferManagerImpl( transports, cache, nfc, events, dh, uh, lh, eh, specialPathManager, batchExecutor );
    }

    public TransferManager getTransfers()
    {
        return transfers;
    }

    public TransportManager getTransports()
    {
        return transports;
    }

    public CacheProvider getCache()
    {
        return cache;
    }

    public FileEventManager getEvents()
    {
        return events;
    }

    public TransferDecoratorManager getDecorator()
    {
        return decorator;
    }

    public ExecutorService getExecutor()
    {
        return executor;
    }

    public NotFoundCache getNotFoundCache()
    {
        return nfc;
    }

}
