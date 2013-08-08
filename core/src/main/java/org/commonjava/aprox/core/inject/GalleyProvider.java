package org.commonjava.aprox.core.inject;

import java.util.concurrent.ExecutorService;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.TransferManagerImpl;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.io.TransferDecorator;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.TransportManagerImpl;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;

public class GalleyProvider
{

    @Inject
    @ExecutorConfig( threads = 4, daemon = true, named = "galley", priority = 8 )
    private ExecutorService executor;

    @Inject
    private Http http;

    @Inject
    private AproxConfiguration config;

    private TransferManager transferManager;

    private TransportManager transportManager;

    private TransferDecorator transferDecorator;

    private FileEventManager fileEventManager;

    @Inject
    private CacheProvider cacheProvider;

    public void setup()
    {
        transportManager = new TransportManagerImpl( new HttpClientTransport( http ) );
        transferManager =
            new TransferManagerImpl( transportManager, cacheProvider, fileEventManager, transferDecorator, executor );
    }

    @Produces
    public TransferManager getTransferManager()
    {
        return transferManager;
    }

}
