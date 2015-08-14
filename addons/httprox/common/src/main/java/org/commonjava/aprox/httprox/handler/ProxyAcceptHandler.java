package org.commonjava.aprox.httprox.handler;

import org.commonjava.aprox.boot.BootOptions;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.httprox.conf.HttproxConfig;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.Pooled;
import org.xnio.StreamConnection;
import org.xnio.channels.AcceptingChannel;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by jdcasey on 8/13/15.
 */
public class ProxyAcceptHandler implements ChannelListener<AcceptingChannel<StreamConnection>>
{
    @Inject
    private HttproxConfig config;

    @Inject
    private BootOptions bootOptions;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private ContentController contentController;

    @Inject
    private CacheProvider cacheProvider;

    public ProxyAcceptHandler(){}

    public ProxyAcceptHandler( HttproxConfig config, BootOptions bootOptions, StoreDataManager storeManager,
                               ContentController contentController, CacheProvider cacheProvider )
    {
        this.config = config;
        this.bootOptions = bootOptions;
        this.storeManager = storeManager;
        this.contentController = contentController;
        this.cacheProvider = cacheProvider;
    }

    @Override
    public void handleEvent( AcceptingChannel<StreamConnection> channel )
    {
        final Logger logger = LoggerFactory.getLogger( getClass() );

        StreamConnection accepted;
        try
        {
            accepted = channel.accept();
        }
        catch ( IOException e )
        {
            logger.error("Failed to accept httprox connection: " + e.getMessage(), e );
            return;
        }

        logger.debug( "accepted {}", accepted.getPeerAddress() );

        final ConduitStreamSourceChannel source = accepted.getSourceChannel();
        final ConduitStreamSinkChannel sink = accepted.getSinkChannel();

        final ProxyResponseWriter writer =
                        new ProxyResponseWriter( config, storeManager, contentController, cacheProvider );

        logger.debug( "Setting writer: {}", writer );
        sink.getWriteSetter()
            .set( writer );

        final ProxyRequestReader reader = new ProxyRequestReader( writer, sink );

        logger.debug( "Setting reader: {}", reader );
        source.getReadSetter()
              .set( reader );

        source.resumeReads();

    }
}
