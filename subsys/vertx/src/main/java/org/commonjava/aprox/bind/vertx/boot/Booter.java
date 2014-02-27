/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.bind.vertx.boot;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.codehaus.plexus.interpolation.InterpolationException;
import org.commonjava.aprox.conf.AproxConfigFactory;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.platform.Verticle;

import ch.qos.logback.core.joran.spi.JoranException;

public class Booter
    extends Verticle
{
    public static final String APROX_HOME_PROP = "aprox.home";

    public static final String BOOT_DEFAULTS_PROP = "aprox.boot.defaults";

    public static final int CANT_LOAD_BOOT_DEFAULTS = 1;

    public static final int CANT_PARSE_ARGS = 2;

    public static final int CANT_INTERP_BOOT_DEFAULTS = 3;

    public static final int CANT_CONFIGURE_LOGGING = 4;

    public static final String APROX_LOGCONF_PROP = "aprox.logging";

    public static void main( final String[] args )
    {
        final String bootDef = System.getProperty( BOOT_DEFAULTS_PROP );
        File bootDefaults = null;
        if ( bootDef != null )
        {
            bootDefaults = new File( bootDef );
        }

        final String logconf = System.getProperty( APROX_LOGCONF_PROP );
        File logConf = null;
        if ( logconf != null )
        {
            logConf = new File( logconf );
        }

        final BootOptions boot = new BootOptions();
        try
        {
            final String aproxHome = System.getProperty( APROX_HOME_PROP, new File( "." ).getCanonicalPath() );

            boot.setDefaults( bootDefaults, aproxHome );
            boot.configureLogging( logConf );
        }
        catch ( final IOException e )
        {
            System.err.printf( "ERROR LOADING BOOT DEFAULTS: %s.\nReason: %s\n\n", bootDefaults, e.getMessage() );
            System.exit( CANT_LOAD_BOOT_DEFAULTS );
        }
        catch ( final InterpolationException e )
        {
            System.err.printf( "ERROR RESOLVING BOOT DEFAULTS: %s.\nReason: %s\n\n", bootDefaults, e.getMessage() );
            System.exit( CANT_INTERP_BOOT_DEFAULTS );
        }
        catch ( final JoranException e )
        {
            System.err.printf( "ERROR CONFIGURING LOGGING FROM: %s.\nReason: %s\n\n", logConf, e.getMessage() );
            System.exit( CANT_CONFIGURE_LOGGING );
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
            System.exit( CANT_PARSE_ARGS );
        }

        if ( boot.isHelp() )
        {
            printUsage( parser, null );
            canStart = false;
        }

        if ( canStart )
        {
            new Booter( boot ).run();
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

    public Booter( final BootOptions bootOptions )
    {
        this.bootOptions = bootOptions;
    }

    public void run()
    {
        if ( bootOptions.getConfig() != null )
        {
            final Properties properties = System.getProperties();

            System.out.printf( "\n\nUsing AProx configuration: %s\n", bootOptions.getConfig() );
            properties.setProperty( AproxConfigFactory.CONFIG_PATH_PROP, bootOptions.getConfig() );
            System.setProperties( properties );
        }

        final Weld weld = new Weld();
        final WeldContainer container = weld.initialize();

        final MasterRouter router = container.instance()
                                             .select( MasterRouter.class )
                                             .get();
        //        router.initializeComponents();

        setVertx( new DefaultVertx() );

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
    }

}
