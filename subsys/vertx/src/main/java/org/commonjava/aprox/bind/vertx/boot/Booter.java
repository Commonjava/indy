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
package org.commonjava.aprox.bind.vertx.boot;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.commonjava.aprox.AproxException;
import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.AproxLifecycleManager;
import org.commonjava.aprox.conf.AproxConfigFactory;
import org.commonjava.web.config.ConfigurationException;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;

public class Booter
{
    public static final String APROX_HOME_PROP = "aprox.home";

    public static final String BOOT_DEFAULTS_PROP = "aprox.boot.defaults";

    public static final int ERR_CANT_LOAD_BOOT_DEFAULTS = 1;

    public static final int ERR_CANT_PARSE_ARGS = 2;

    public static final int ERR_CANT_INTERP_BOOT_DEFAULTS = 3;

    public static final int ERR_CANT_CONFIGURE_LOGGING = 4;

    private static final int ERR_CANT_CONFIGURE_APROX = 5;

    private static final int ERR_CANT_START_APROX = 6;

    protected static final int ERR_CANT_LISTEN = 7;

    private static final int ERR_CANT_INIT_BOOTER = 8;

    public static void main( final String[] args )
    {
        final String bootDef = System.getProperty( BOOT_DEFAULTS_PROP );
        File bootDefaults = null;
        if ( bootDef != null )
        {
            bootDefaults = new File( bootDef );
        }

        final BootOptions boot;
        try
        {
            final String aproxHome = System.getProperty( APROX_HOME_PROP, new File( "." ).getCanonicalPath() );

            boot = new BootOptions( bootDefaults, aproxHome );
        }
        catch ( final IOException e )
        {
            System.err.printf( "ERROR LOADING BOOT DEFAULTS: %s.\nReason: %s\n\n", bootDefaults, e.getMessage() );
            System.exit( ERR_CANT_LOAD_BOOT_DEFAULTS );
            return;
        }
        catch ( final InterpolationException e )
        {
            System.err.printf( "ERROR RESOLVING BOOT DEFAULTS: %s.\nReason: %s\n\n", bootDefaults, e.getMessage() );
            System.exit( ERR_CANT_INTERP_BOOT_DEFAULTS );
            return;
        }

        final CmdLineParser parser = new CmdLineParser( boot );
        boolean canStart = true;
        try
        {
            parser.parseArgument( args );
        }
        catch ( final CmdLineException e )
        {
            System.err.printf( "ERROR: %s", e.getMessage() );
            printUsage( parser, e );
            System.exit( ERR_CANT_PARSE_ARGS );
        }

        if ( boot.isHelp() )
        {
            printUsage( parser, null );
            canStart = false;
        }

        if ( canStart )
        {
            try
            {
                final Booter booter = new Booter( boot );

                System.out.println( "Starting AProx booter: " + booter );
                final int result = booter.runAndWait();
                if ( result != 0 )
                {
                    System.exit( result );
                }
            }
            catch ( final AproxException e )
            {
                System.err.printf( "ERROR INITIALIZING BOOTER: %s", e.getMessage() );
                System.exit( ERR_CANT_INIT_BOOTER );
            }
        }
    }

    public static void printUsage( final CmdLineParser parser, final CmdLineException error )
    {
        if ( error != null )
        {
            System.err.println( "Invalid option(s): " + error.getMessage() );
            System.err.println();
        }

        System.err.println( "Usage: $0 [OPTIONS] [<target-path>]" );
        System.err.println();
        System.err.println();
        // If we are running under a Linux shell COLUMNS might be available for the width
        // of the terminal.
        parser.setUsageWidth( ( System.getenv( "COLUMNS" ) == null ? 100 : Integer.valueOf( System.getenv( "COLUMNS" ) ) ) );
        parser.printUsage( System.err );
        System.err.println();
    }

    public static void setConfigPathProperty( final String config )
    {
        final Properties properties = System.getProperties();

        System.out.printf( "\n\nUsing AProx configuration: %s\n", config );
        properties.setProperty( AproxConfigFactory.CONFIG_PATH_PROP, config );
        System.setProperties( properties );
    }

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final BootOptions bootOptions;

    private int exit = 0;

    private Vertx vertx;

    private final Weld weld;

    private final WeldContainer container;

    private AproxLifecycleManager lifecycleManager;

    private AproxConfigFactory configFactory;

    private MasterRouter router;

    private BootStatus status;

    public Booter( final BootOptions bootOptions )
        throws AproxException
    {
        this.bootOptions = bootOptions;

        try
        {
            if ( bootOptions.getConfig() != null )
            {
                setConfigPathProperty( bootOptions.getConfig() );
            }

            weld = new Weld();
            container = weld.initialize();
        }
        catch ( final RuntimeException e )
        {
            throw new AproxException( "Failed to initialize Booter: " + e.getMessage(), e );
        }
    }

    private int runAndWait()
    {
        start();

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

    public WeldContainer getContainer()
    {
        return container;
    }

    public BootStatus start()
    {
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

    public void stop()
    {
        if ( container != null )
        {
            weld.shutdown();
        }
    }

}
