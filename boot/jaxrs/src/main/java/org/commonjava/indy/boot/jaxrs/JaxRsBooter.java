/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.boot.jaxrs;

import io.undertow.Undertow;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import org.commonjava.atservice.annotation.Service;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.IndyLifecycleManager;
import org.commonjava.indy.bind.jaxrs.IndyDeployment;
import org.commonjava.indy.boot.BootInterface;
import org.commonjava.indy.boot.BootOptions;
import org.commonjava.indy.boot.BootStatus;
import org.commonjava.indy.boot.IndyBootException;
import org.commonjava.indy.boot.PortFinder;
import org.commonjava.indy.boot.WeldBootInterface;
import org.commonjava.indy.conf.IndyConfigFactory;
import org.commonjava.web.config.ConfigurationException;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Options;

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletException;
import java.lang.reflect.InvocationTargetException;

@Service( BootInterface.class )
public class JaxRsBooter
    implements WeldBootInterface
{
    public static void main( final String[] args )
    {
        Thread.currentThread()
              .setUncaughtExceptionHandler( ( thread, error ) -> {
                  if ( error instanceof InvocationTargetException )
                  {
                      final InvocationTargetException ite = (InvocationTargetException) error;
                      System.err.println( "In: " + thread.getName() + "(" + thread.getId()
                          + "), caught InvocationTargetException:" );
                      ite.getTargetException()
                         .printStackTrace();

                      System.err.println( "...via:" );
                      error.printStackTrace();
                  }
                  else
                  {
                      System.err.println( "In: " + thread.getName() + "(" + thread.getId() + ") Uncaught error:" );
                      error.printStackTrace();
                  }
              } );

        BootOptions boot;
        try
        {
            boot = BootOptions.loadFromSysprops();
        }
        catch ( final IndyBootException e )
        {
            System.err.printf( "ERROR: %s", e.getMessage() );
            System.exit( ERR_CANT_LOAD_BOOT_OPTIONS );
            return;
        }

        try
        {
            if ( boot.parseArgs( args ) )
            {
                try
                {
                    final BootInterface booter = new JaxRsBooter();

                    System.out.println( "Starting Indy booter: " + booter );
                    final int result = booter.runAndWait( boot );
                    if ( result != 0 )
                    {
                        System.exit( result );
                    }
                }
                catch ( final IndyBootException e )
                {
                    System.err.printf( "ERROR INITIALIZING BOOTER: %s", e.getMessage() );
                    System.exit( ERR_CANT_INIT_BOOTER );
                }
            }
        }
        catch ( final IndyBootException e )
        {
            System.err.printf( "ERROR: %s", e.getMessage() );
            System.exit( ERR_CANT_PARSE_ARGS );
        }
    }

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private BootOptions bootOptions;

    private int exit = 0;

    private Undertow server;

    private Weld weld;

    private WeldContainer container;

    private IndyLifecycleManager lifecycleManager;

    private IndyConfigFactory configFactory;

    private BootStatus status;

    @Override
    public boolean initialize( final BootOptions bootOptions )
    {
        this.bootOptions = bootOptions;

        boolean initialized;
        try
        {
            bootOptions.setSystemProperties();

            weld = new Weld();
            weld.property("org.jboss.weld.se.archive.isolation", false);

            // Weld shutdown hook might be called before Indy's, we need to disable it to allow Indy's shutdown hooks execute smoothly
            weld.skipShutdownHook();

            container = weld.initialize();

            // injectable version.
            final BootOptions cdiOptions = container.select( BootOptions.class )
                                                    .get();
            cdiOptions.copyFrom( bootOptions );

            final BeanManager bmgr = container.getBeanManager();
            logger.info( "\n\n\nStarted BeanManager: {}\n\n\n", bmgr );
            initialized = true;
        }
        catch ( final Throwable e )
        {
            logger.error( "Failed to initialize Booter: " + e.getMessage(), e );
            exit = ERR_CANT_INIT_BOOTER;
            status = new BootStatus( exit, e );
            initialized = false;
        }

        return initialized;
    }

    @Override
    public boolean loadConfiguration( final String config )
    {
        logger.info( "Booter running: " + this );

        configFactory = container.instance()
                                 .select( IndyConfigFactory.class )
                                 .get();

        boolean loaded;
        try
        {
            logger.info( "\n\nLoading Indy configuration factory: {}\n", configFactory );
            configFactory.load( bootOptions.getConfig() );
            loaded = true;
        }
        catch ( final ConfigurationException e )
        {
            logger.error( "Failed to configure Indy: {}", e.getMessage() );
            e.printStackTrace();
            exit = ERR_CANT_CONFIGURE_INDY;
            status = new BootStatus( exit, e );
            loaded = false;
        }

        return loaded;
    }

    @Override
    public boolean startLifecycle()
    {
        lifecycleManager = container.select( IndyLifecycleManager.class ).get();

        boolean started;
        try
        {
            lifecycleManager.start();
            started = true;
        }
        catch ( final IndyLifecycleException e )
        {
            logger.error( "\n\nFailed to start Indy: " + e.getMessage(), e );

            exit = ERR_CANT_START_INDY;
            status = new BootStatus( exit, e );
            started = false;
        }

        return started;
    }

    @Override
    public boolean deploy()
    {
        boolean started;
        final IndyDeployment indyDeployment = container.select( IndyDeployment.class ).get();

        final DeploymentInfo di = indyDeployment.getDeployment( bootOptions.getContextPath() )
                                                 .setContextPath( "/" );

        final DeploymentManager dm = Servlets.defaultContainer()
                                             .addDeployment( di );
        dm.deploy();

        final RestConfig config = container.select( RestConfig.class ).get();

        status = new BootStatus();
        try
        {
            Integer port = bootOptions.getPort();
            if ( port < 1 )
            {
                System.out.println("Looking for open port...");

                final ThreadLocal<ServletException> errorHolder = new ThreadLocal<>();
                ThreadLocal<Integer> usingPort = new ThreadLocal<>();

                server = PortFinder.findPortFor( 16, ( foundPort ) -> {
                    Undertow undertow = null;
                    try
                    {
                        EncodingHandler eh = getGzipEncodeHandler( dm );
                        Undertow.Builder builder =
                                Undertow.builder().setHandler( eh ).addHttpListener( foundPort, bootOptions.getBind() );

                        builder.setWorkerOption( Options.WORKER_NAME, "REST" );
                        config.configureBuilder( builder );

                        undertow = builder.build();

                        undertow.start();
                        usingPort.set( foundPort );
                    }
                    catch ( ServletException e )
                    {
                        errorHolder.set( e );
                    }

                    return undertow;
                } );

                ServletException e = errorHolder.get();
                if ( e != null )
                {
                    throw e;
                }

                bootOptions.setPort( usingPort.get() );
            }
            else
            {
                server = Undertow.builder()
                                 .setHandler( getGzipEncodeHandler( dm ) )
                                 .addHttpListener( port, bootOptions.getBind() )
                                 .build();


                server.start();
            }

            System.out.println( "Using: " + bootOptions.getPort() );

            status.markSuccess();
            started = true;

            System.out.printf( "Indy listening on %s:%s\n\n", bootOptions.getBind(), bootOptions.getPort() );

        }
        catch ( ServletException | RuntimeException e )
        {
            status.markFailed( ERR_CANT_LISTEN, e );
            started = false;
        }

        return started;
    }

    private EncodingHandler getGzipEncodeHandler(final DeploymentManager dm) throws ServletException{
        // FROM: https://stackoverflow.com/questions/28295752/compressing-undertow-server-responses#28329810
        final Predicate sizePredicate = Predicates.parse( "max-content-size[" + Long.toString( 5 * 1024 ) + "]" );

        EncodingHandler eh = new EncodingHandler(
                new ContentEncodingRepository().addEncodingHandler( "gzip", new GzipEncodingProvider(), 50,
                                                                    sizePredicate )
                                               .addEncodingHandler( "deflate", new DeflateEncodingProvider(), 51,
                                                                    sizePredicate ) ).setNext( dm.start() );
        return eh;
    }

    @Override
    public WeldContainer getContainer()
    {
        return container;
    }

    @Override
    public BootOptions getBootOptions()
    {
        return bootOptions;
    }

    @Override
    public BootStatus start( final BootOptions bootOptions )
        throws IndyBootException
    {
        if ( !initialize( bootOptions ) )
        {
            return status;
        }

        if ( !loadConfiguration( bootOptions.getConfig() ) )
        {
            return status;
        }

        if ( !startLifecycle() )
        {
            return status;
        }

        deploy();

        return status;
    }

    @Override
    public void stop()
    {
        if ( container != null )
        {
            server.stop();

            try
            {
                lifecycleManager.stop();
            }
            catch ( final IndyLifecycleException e )
            {
                logger.error( "Failed to run stop actions for lifecycle: " + e.getMessage(), e );
            }

            weld.shutdown();
        }
    }

    @Override
    public int runAndWait( final BootOptions bootOptions )
        throws IndyBootException
    {
        start( bootOptions );

        if ( server != null )
        {
            logger.info( "Setting up shutdown hook..." );
            Runtime.getRuntime()
                   .addShutdownHook( new Thread( lifecycleManager.createShutdownRunnable( server ) ) );

            synchronized ( server )
            {
                try
                {
                    server.wait();
                }
                catch ( final InterruptedException e )
                {
                    e.printStackTrace();
                    logger.info( "Indy exiting" );
                }
            }
        }

        return exit;
    }

}
