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
import org.commonjava.aprox.conf.AproxConfigFactory;
import org.commonjava.web.config.ConfigurationException;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.impl.DefaultVertx;

public class Booter
{
    public static final String APROX_HOME_PROP = "aprox.home";

    public static final String BOOT_DEFAULTS_PROP = "aprox.boot.defaults";

    public static final int ERR_CANT_LOAD_BOOT_DEFAULTS = 1;

    public static final int ERR_CANT_PARSE_ARGS = 2;

    public static final int ERR_CANT_INTERP_BOOT_DEFAULTS = 3;

    public static final int ERR_CANT_CONFIGURE_LOGGING = 4;

    private static final int ERR_CANT_CONFIGURE_APROX = 5;

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
            final Booter booter = new Booter( boot );
            System.out.println( "Starting AProx booter: " + booter );
            final int result = booter.run();
            if ( result != 0 )
            {
                System.exit( result );
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

    private final BootOptions bootOptions;

    private Booter( final BootOptions bootOptions )
    {
        this.bootOptions = bootOptions;
    }

    private int run()
    {
        System.out.println( "Booter running: " + this );

        if ( bootOptions.getConfig() != null )
        {
            final Properties properties = System.getProperties();

            System.out.printf( "\n\nUsing AProx configuration: %s\n", bootOptions.getConfig() );
            properties.setProperty( AproxConfigFactory.CONFIG_PATH_PROP, bootOptions.getConfig() );
            System.setProperties( properties );
        }

        final Weld weld = new Weld();
        final WeldContainer container = weld.initialize();

        final AproxConfigFactory configFactory = container.instance()
                                                          .select( AproxConfigFactory.class )
                                                          .get();
        try
        {
            System.out.printf( "\n\nLoading AProx configuration factory: %s\n", configFactory );
            configFactory.load( bootOptions.getConfig() );
        }
        catch ( final ConfigurationException e )
        {
            System.err.printf( "Failed to configure AProx: %s", e.getMessage() );
            e.printStackTrace();
            return ERR_CANT_CONFIGURE_APROX;
        }

        final MasterRouter router = container.instance()
                                             .select( MasterRouter.class )
                                             .get();

        router.setPrefix( bootOptions.getContextPath() );

        //        router.initializeComponents();

        final DefaultVertx vertx = new DefaultVertx();

        for ( int i = 0; i < bootOptions.getWorkers(); i++ )
        {
            final HttpServer server = vertx.createHttpServer();
            server.requestHandler( router )
                  .listen( bootOptions.getPort(), bootOptions.getBind() );
        }

        System.out.printf( "AProx: %s workers listening on %s:%s\n\n", bootOptions.getWorkers(), bootOptions.getBind(), bootOptions.getPort() );

        synchronized ( this )
        {
            try
            {
                wait();
            }
            catch ( final InterruptedException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return 0;
    }

}
