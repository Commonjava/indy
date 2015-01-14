/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.boot.vertx;

import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.AproxLifecycleManager;
import org.commonjava.aprox.bind.vertx.MasterRouter;
import org.commonjava.aprox.boot.AproxBootException;
import org.commonjava.aprox.boot.BootInterface;
import org.commonjava.aprox.boot.BootOptions;
import org.commonjava.aprox.boot.BootStatus;
import org.commonjava.aprox.boot.WeldBootInterface;
import org.commonjava.aprox.conf.AproxConfigFactory;
import org.commonjava.atservice.annotation.Service;
import org.commonjava.web.config.ConfigurationException;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;

@Service( BootInterface.class )
public class VertxBooter
    implements WeldBootInterface
{
    public static void main( final String[] args )
    {
        BootOptions boot;
        try
        {
            boot = BootOptions.loadFromSysprops();
        }
        catch ( final AproxBootException e )
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
                    final VertxBooter booter = new VertxBooter();

                    System.out.println( "Starting AProx booter: " + booter );
                    final int result = booter.runAndWait( boot );
                    if ( result != 0 )
                    {
                        System.exit( result );
                    }
                }
                catch ( final AproxBootException e )
                {
                    System.err.printf( "ERROR INITIALIZING BOOTER: %s", e.getMessage() );
                    System.exit( ERR_CANT_INIT_BOOTER );
                }
            }
        }
        catch ( final AproxBootException e )
        {
            System.err.printf( "ERROR: %s", e.getMessage() );
            System.exit( ERR_CANT_PARSE_ARGS );
        }
    }

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private BootOptions bootOptions;

    private int exit = 0;

    private Vertx vertx;

    private Weld weld;

    private WeldContainer container;

    private AproxLifecycleManager lifecycleManager;

    private AproxConfigFactory configFactory;

    private MasterRouter router;

    private BootStatus status;

    private void initialize( final BootOptions bootOptions )
        throws AproxBootException
    {
        this.bootOptions = bootOptions;

        try
        {
            bootOptions.setSystemProperties();

            weld = new Weld();
            container = weld.initialize();
        }
        catch ( final RuntimeException e )
        {
            throw new AproxBootException( "Failed to initialize Booter: " + e.getMessage(), e );
        }
    }

    public int runAndWait( final BootOptions bootOptions )
        throws AproxBootException
    {
        start( bootOptions );

        logger.info( "Setting up shutdown hook..." );
        Runtime.getRuntime()
               .addShutdownHook( new Thread( lifecycleManager.createShutdownRunnable() ) );

        synchronized ( vertx )
        {
            try
            {
                vertx.wait();
            }
            catch ( final InterruptedException e )
            {
                e.printStackTrace();
                logger.info( "AProx exiting" );
            }
        }

        return exit;
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

    /* (non-Javadoc)
     * @see org.commonjava.aprox.bind.vertx.boot.BootInterface#start()
     */
    @Override
    public BootStatus start( final BootOptions bootOptions )
        throws AproxBootException
    {
        initialize( bootOptions );
        logger.info( "Booter running: " + this );

        configFactory = container.instance()
                                 .select( AproxConfigFactory.class )
                                 .get();
        try
        {
            logger.info( "\n\nLoading AProx configuration factory: {}\n", configFactory );
            configFactory.load( bootOptions.getConfig() );
        }
        catch ( final ConfigurationException e )
        {
            logger.error( "Failed to configure AProx: {}", e.getMessage() );
            e.printStackTrace();
            exit = ERR_CANT_CONFIGURE_APROX;
            status = new BootStatus( exit, e );
            return status;
        }

        lifecycleManager = container.instance()
                                    .select( AproxLifecycleManager.class )
                                    .get();
        try
        {
            lifecycleManager.start();
        }
        catch ( final AproxLifecycleException e )
        {
            logger.error( "\n\nFailed to start AProx: {}", e.getMessage() );
            e.printStackTrace();

            exit = ERR_CANT_START_APROX;
            status = new BootStatus( exit, e );
            return status;
        }

        router = container.instance()
                          .select( MasterRouter.class )
                          .get();

        router.setPrefix( bootOptions.getContextPath() );

        vertx = container.instance()
                         .select( Vertx.class )
                         .get();

        //        for ( int i = 0; i < bootOptions.getWorkers(); i++ )
        //        {
        status = new BootStatus();
        final HttpServer server = vertx.createHttpServer();
        server.requestHandler( router )
              .listen( bootOptions.getPort(), bootOptions.getBind(), new Handler<AsyncResult<HttpServer>>()
              {
                  @Override
                  public void handle( final AsyncResult<HttpServer> event )
                  {
                      if ( event.failed() )
                      {
                          logger.error( "HTTP server failure:\n\n", event.cause() );
                          status.markFailed( ERR_CANT_LISTEN, event.cause() );

                          server.close( new Handler<AsyncResult<Void>>()
                          {
                              @Override
                              public void handle( final AsyncResult<Void> event )
                              {
                                  logger.info( "Shutdown complete." );
                                  synchronized ( status )
                                  {
                                      status.notifyAll();
                                  }
                              }
                          } );
                      }
                      else
                      {
                          status.markSuccess();
                          synchronized ( status )
                          {
                              status.notifyAll();
                          }
                      }
                  }
              } );
        //        }
        //
        //        System.out.printf( "AProx: %s workers listening on %s:%s\n\n", bootOptions.getWorkers(), bootOptions.getBind(),
        System.out.printf( "AProx listening on %s:%s\n\n", bootOptions.getBind(), bootOptions.getPort() );

        while ( !status.isSet() )
        {
            synchronized ( status )
            {
                try
                {
                    status.wait();
                }
                catch ( final InterruptedException e )
                {
                    logger.warn( "Interrupt received! Quitting." );
                    Thread.currentThread()
                          .interrupt();
                    break;
                }
            }
        }

        return status;
    }

    /* (non-Javadoc)
     * @see org.commonjava.aprox.bind.vertx.boot.BootInterface#stop()
     */
    @Override
    public void stop()
    {
        if ( container != null )
        {
            weld.shutdown();
        }
    }

}
