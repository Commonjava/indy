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
import org.commonjava.maven.galley.internal.xfer.DownloadHandler;
import org.commonjava.maven.galley.internal.xfer.ExistenceHandler;
import org.commonjava.maven.galley.internal.xfer.ListingHandler;
import org.commonjava.maven.galley.internal.xfer.UploadHandler;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.TransportManagerImpl;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;

public class GalleyFixture
{

    private final TransferManager transfers;

    private final TransportManager transports;

    private final CacheProvider cache;

    private final FileEventManager events;

    private final TransferDecorator decorator;

    private final ExecutorService executor;

    private final NotFoundCache nfc;

    private final ExecutorService batchExecutor;

    public GalleyFixture( final File repoRoot )
    {
        final AproxHttpProvider aproxHttp = new AproxHttpProvider();
        aproxHttp.setup();

        transports = new TransportManagerImpl( new HttpClientTransport( aproxHttp.getHttpComponent() ) );

        events = new AproxFileEventManager();
        decorator = new NoOpTransferDecorator();
        cache = new FileCacheProvider( repoRoot, new KeyBasedPathGenerator(), events, decorator );
        executor = Executors.newFixedThreadPool( 2 );
        batchExecutor = Executors.newFixedThreadPool( 2 );
        nfc = new MemoryNotFoundCache();

        final DownloadHandler dh = new DownloadHandler( nfc, executor );
        final UploadHandler uh = new UploadHandler( nfc, executor );
        final ListingHandler lh = new ListingHandler( nfc );
        final ExistenceHandler eh = new ExistenceHandler( nfc );

        transfers = new TransferManagerImpl( transports, cache, nfc, events, dh, uh, lh, eh, batchExecutor );
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

    public NotFoundCache getNotFoundCache()
    {
        return nfc;
    }

}
