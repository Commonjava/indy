package org.commonjava.aprox.fixture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.commonjava.aprox.change.event.AproxFileEventManager;
import org.commonjava.aprox.filer.KeyBasedPathGenerator;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.TransferManagerImpl;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.SimpleTransportManager;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;

public class GalleyFixture
{

    private final TransferManager transfers;

    private final TransportManager transports;

    private final CacheProvider cache;

    private final FileEventManager events;

    private final TransferDecorator decorator;

    private final ExecutorService executor;

    public GalleyFixture( final File repoRoot )
    {
        final AproxHttpProvider aproxHttp = new AproxHttpProvider().setup();
        transports = new SimpleTransportManager( new HttpClientTransport( aproxHttp.getHttpComponent() ) );

        cache = new FileCacheProvider( repoRoot, new KeyBasedPathGenerator() );
        events = new AproxFileEventManager();
        decorator = new NoOpTransferDecorator();
        executor = Executors.newFixedThreadPool( 2 );

        transfers = new TransferManagerImpl( transports, cache, events, decorator, executor );
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

    public TransferDecorator getDecorator()
    {
        return decorator;
    }

    public ExecutorService getExecutor()
    {
        return executor;
    }

}
