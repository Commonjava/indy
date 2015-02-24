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
package org.commonjava.aprox.boot.jaxrs;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

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
                    final JaxRsBooter booter = new JaxRsBooter();

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

    private void initialize( final BootOptions bootOptions )
        throws AproxBootException
    {
        this.bootOptions = bootOptions;

        try
        {
            bootOptions.setSystemProperties();

            weld = new Weld();
            container = weld.initialize();

            final BeanManager bmgr = container.getBeanManager();
            logger.info( "\n\n\nStarted BeanManager: {}\n\n\n", bmgr );
        }
        catch ( final RuntimeException e )
        {
            throw new AproxBootException( "Failed to initialize Booter: " + e.getMessage(), e );
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

        final AproxDeployment aproxDeployment = container.instance()
                                                         .select( AproxDeployment.class )
                                                         .get();

        final DeploymentInfo di = aproxDeployment.getDeployment( bootOptions.getContextPath() )
                                                 .setContextPath( "/" );

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
        }
        catch ( ServletException | RuntimeException e )
        {
            status.markFailed( ERR_CANT_LISTEN, e );
        }


        System.out.printf( "AProx listening on %s:%s\n\n", bootOptions.getBind(), bootOptions.getPort() );

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
            weld.shutdown();
        }
    }

}
