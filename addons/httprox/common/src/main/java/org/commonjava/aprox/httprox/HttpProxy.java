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
package org.commonjava.aprox.httprox;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.ShutdownAction;
import org.commonjava.aprox.action.StartupAction;
import org.commonjava.aprox.boot.BootOptions;
import org.commonjava.aprox.core.ctl.ContentController;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.httprox.conf.HttproxConfig;
import org.commonjava.aprox.httprox.handler.ProxyRequestReader;
import org.commonjava.aprox.httprox.handler.ProxyResponseWriter;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
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
    private CacheProvider cacheProvider;

    private AcceptingChannel<StreamConnection> server;

    protected HttpProxy()
    {
    }

    public HttpProxy( final HttproxConfig config, final BootOptions bootOptions, final StoreDataManager storeManager,
                      final ContentController contentController, final CacheProvider cacheProvider )
    {
        this.config = config;
        this.bootOptions = bootOptions;
        this.storeManager = storeManager;
        this.contentController = contentController;
        this.cacheProvider = cacheProvider;
    }

    @Override
    public void start()
        throws AproxLifecycleException
    {
        if ( !config.isEnabled() )
        {
            logger.info( "HTTProx proxy is disabled." );
            return;
        }

        XnioWorker worker;
        try
        {
            worker = Xnio.getInstance()
                         .createWorker( OptionMap.EMPTY );

            String bind = bootOptions.getBind();
            if ( bind == null )
            {
                bind = "0.0.0.0";
            }

            logger.info( "Starting HTTProx proxy on: {}:{}", bind, config.getPort() );
            final InetSocketAddress addr = new InetSocketAddress( bind, config.getPort() );
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
        if ( server != null )
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
                
                final ProxyResponseWriter writer =
                    new ProxyResponseWriter( config, storeManager, contentController, cacheProvider );
                final ProxyRequestReader reader = new ProxyRequestReader( writer, sink );

                logger.debug( "Setting reader: {}", reader );
                source.getReadSetter()
                      .set( reader );

                logger.debug( "Setting writer: {}", writer );
                sink.getWriteSetter()
                    .set( writer );

                accepted.getCloseSetter()
                        .set( new ChannelListener<StreamConnection>()
                        {
                            @Override
                            public void handleEvent( final StreamConnection channel )
                            {
                                IOUtils.closeQuietly( channel );
                            }
                        } );

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
