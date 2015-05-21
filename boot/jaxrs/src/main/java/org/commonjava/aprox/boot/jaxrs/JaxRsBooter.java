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
package org.commonjava.aprox.boot.jaxrs;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;

import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletException;

import org.commonjava.aprox.action.AproxLifecycleException;
import org.commonjava.aprox.action.AproxLifecycleManager;
import org.commonjava.aprox.bind.jaxrs.AproxDeployment;
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

@Service( BootInterface.class )
public class JaxRsBooter
    implements WeldBootInterface
{
    public static void main( final String[] args )
    {
        Thread.currentThread()
              .setUncaughtExceptionHandler( new UncaughtExceptionHandler()
              {
                  @Override
                  public void uncaughtException( final Thread thread, final Throwable error )
                  {
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
                  }
              } );

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
                    final BootInterface booter = new JaxRsBooter();

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

    private Undertow server;

    private Weld weld;

    private WeldContainer container;

    private AproxLifecycleManager lifecycleManager;

    private AproxConfigFactory configFactory;

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
            container = weld.initialize();

            // injectable version.
            final BootOptions cdiOptions = container.instance()
                                                    .select( BootOptions.class )
                                                    .get();
            cdiOptions.copyFrom( bootOptions );

            final BeanManager bmgr = container.getBeanManager();
            logger.info( "\n\n\nStarted BeanManager: {}\n\n\n", bmgr );
            initialized = true;
        }
        catch ( final RuntimeException e )
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
                                 .select( AproxConfigFactory.class )
                                 .get();

        boolean loaded;
        try
        {
            logger.info( "\n\nLoading AProx configuration factory: {}\n", configFactory );
            configFactory.load( bootOptions.getConfig() );
            loaded = true;
        }
        catch ( final ConfigurationException e )
        {
            logger.error( "Failed to configure AProx: {}", e.getMessage() );
            e.printStackTrace();
            exit = ERR_CANT_CONFIGURE_APROX;
            status = new BootStatus( exit, e );
            loaded = false;
        }

        return loaded;
    }

    @Override
    public boolean startLifecycle()
    {
        lifecycleManager = container.instance()
                                    .select( AproxLifecycleManager.class )
                                    .get();

        boolean started;
        try
        {
            lifecycleManager.start();
            started = true;
        }
        catch ( final AproxLifecycleException e )
        {
            logger.error( "\n\nFailed to start AProx: " + e.getMessage(), e );

            exit = ERR_CANT_START_APROX;
            status = new BootStatus( exit, e );
            started = false;
        }

        return started;
    }

    @Override
    public boolean deploy()
    {
        boolean started;
        final AproxDeployment aproxDeployment = container.instance()
                                                         .select( AproxDeployment.class )
                                                         .get();

        DeploymentInfo di = aproxDeployment.getDeployment( bootOptions.getContextPath() )
                                                 .setContextPath( "/" );
        
        if(bootOptions.isSecure()) {
        	System.out.println(">>> Using Aprox secure mode <<<");
        	System.out.println(">>> secure config: " + bootOptions.getSecureConfig());
        	System.out.println(">>> secure realm: " + bootOptions.getRealm());
        	di = aproxDeployment.addKeycloakAdapterToDeployment(di, bootOptions.getSecureConfig(), bootOptions.getRealm());
        	// TODO define proper roles here & possibly read them from boot configuration as well 
        	di = aproxDeployment.addSecurityConstraintToDeployment(di, "admin", "/api/*");
        }
        final DeploymentManager dm = Servlets.defaultContainer()
                                             .addDeployment( di );
        dm.deploy();

        status = new BootStatus();
        try
        {
            server = Undertow.builder()
                             .setHandler( dm.start() )
                             .addHttpListener( bootOptions.getPort(), bootOptions.getBind() )
                             .build();

            server.start();
            status.markSuccess();
            started = true;

            System.out.printf( "AProx listening on %s:%s\n\n", bootOptions.getBind(), bootOptions.getPort() );

        }
        catch ( ServletException | RuntimeException e )
        {
            status.markFailed( ERR_CANT_LISTEN, e );
            started = false;
        }

        return started;
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

    /* (non-Javadoc)
     * @see org.commonjava.aprox.bind.vertx.boot.BootInterface#stop()
     */
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
            catch ( final AproxLifecycleException e )
            {
                logger.error( "Failed to run stop actions for lifecycle: " + e.getMessage(), e );
            }

            weld.shutdown();
        }
    }

    @Override
    public int runAndWait( final BootOptions bootOptions )
        throws AproxBootException
    {
        start( bootOptions );

        logger.info( "Setting up shutdown hook..." );
        Runtime.getRuntime()
               .addShutdownHook( new Thread( lifecycleManager.createShutdownRunnable() ) );

        synchronized ( server )
        {
            try
            {
                server.wait();
            }
            catch ( final InterruptedException e )
            {
                e.printStackTrace();
                logger.info( "AProx exiting" );
            }
        }

        return exit;
    }

}
