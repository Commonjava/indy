package org.commonjava.aprox.httprox;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.ShutdownAction;
import org.commonjava.aprox.action.StartupAction;
import org.commonjava.aprox.boot.BootOptions;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.folo.ctl.FoloContentController;
import org.commonjava.aprox.httprox.conf.HttproxConfig;
import org.commonjava.aprox.httprox.handler.ProxyRequestReader;
import org.commonjava.aprox.httprox.handler.ProxyResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.OptionMap;
import org.xnio.StreamConnection;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.conduits.ConduitStreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

@ApplicationScoped
public class HttpProxy
    implements ChannelListener<AcceptingChannel<StreamConnection>>, StartupAction, ShutdownAction
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private HttproxConfig config;

    @Inject
    private BootOptions bootOptions;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private ContentController contentController;

    @Inject
    private FoloContentController foloContentController;

    private AcceptingChannel<StreamConnection> server;

    protected HttpProxy()
    {
    }

    public HttpProxy( final HttproxConfig config, final BootOptions bootOptions, final StoreDataManager storeManager,
                      final ContentController contentController )
    {
        this.config = config;
        this.bootOptions = bootOptions;
        this.storeManager = storeManager;
        this.contentController = contentController;
    }

    @Override
    public void start()
        throws AproxLifecycleException
    {
        XnioWorker worker;
        try
        {
            worker = Xnio.getInstance()
                         .createWorker( OptionMap.EMPTY );

            final InetSocketAddress addr = new InetSocketAddress( bootOptions.getBind(), config.getPort() );
            server = worker.createStreamConnectionServer( addr, this, OptionMap.EMPTY );

            server.resumeAccepts();
            logger.info( "HTTProxy listening on: {}", addr );
        }
        catch ( IllegalArgumentException | IOException e )
        {
            throw new AproxLifecycleException( "Failed to start HTTProx general content proxy: %s", e, e.getMessage() );
        }
    }

    @Override
    public void stop()
    {
        try
        {
            logger.info( "stopping server" );
            server.suspendAccepts();
            server.close();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to stop: " + e.getMessage(), e );
        }
    }

    @Override
    public void handleEvent( final AcceptingChannel<StreamConnection> channel )
    {
        try
        {
            StreamConnection accepted;
            while ( ( accepted = channel.accept() ) != null )
            {
                logger.debug( "accepted {}", accepted.getPeerAddress() );

                final ConduitStreamSourceChannel source = accepted.getSourceChannel();
                final ConduitStreamSinkChannel sink = accepted.getSinkChannel();
                
                final ProxyResponseWriter writer = new ProxyResponseWriter(config, storeManager, contentController);
                final ProxyRequestReader reader = new ProxyRequestReader( writer, sink );

                logger.debug( "Setting reader: {}", reader );
                source.getReadSetter()
                      .set( reader );

                logger.debug( "Setting writer: {}", writer );
                sink.getWriteSetter()
                    .set( writer );

                source.resumeReads();
            }

            channel.resumeAccepts();
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to accept httprox requests: " + e.getMessage(), e );
        }
    }

    @Override
    public String getId()
    {
        return "httproxy-listener";
    }

    @Override
    public int getStartupPriority()
    {
        return 1;
    }

    @Override
    public int getShutdownPriority()
    {
        return 99;
    }

}
