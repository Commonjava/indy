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
package org.commonjava.indy.boot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class BootOptions
{

    public static final String BIND_PROP = "bind";

    public static final String PORT_PROP = "port";

    public static final String CONFIG_PROP = "config";

    public static final String WORKERS_PROP = "workers";

    public static final String CONTEXT_PATH_PROP = "context-path";

    public static final String DEFAULT_BIND = "0.0.0.0";

    public static final int DEFAULT_PORT = 8080;

    @Option( name = "-h", aliases = { "--help" }, usage = "Print this and exit" )
    private boolean help;

    @Option( name = "-i", aliases = { "--interface", "--bind", "--listen" }, usage = "Bind to a particular IP address (default: 0.0.0.0, or all available)" )
    private String bind;

    @Option( name = "-p", aliases = { "--port" }, usage = "Use different port (default: 8080)" )
    private Integer port;

    @Option( name = "-c", aliases = { "--config" }, usage = "Use an alternative configuration file (default: <indy-home>/etc/indy/main.conf)" )
    private String config;

    @Option( name = "-C", aliases = { "--context-path" }, usage = "Specify a root context path for all of indy to use" )
    private String contextPath;
    
    @Option( name = "-S", aliases = { "--secure-config-path" }, usage = "Specify config path of security file" )
    private String secureConfig;
    
    @Option( name = "-R", aliases = { "--secure-realm" }, usage = "Specify security realm" )
    private String secureRealm;
    
    private StringSearchInterpolator interp;

    private Properties bootProps;

    private String indyHome;
    
    public static BootOptions loadFromSysprops()
        throws IndyBootException
    {
        final String bootDef = System.getProperty( BootInterface.BOOT_DEFAULTS_PROP );
        File bootDefaults = null;
        if ( bootDef != null )
        {
            bootDefaults = new File( bootDef );
        }
        
        try
        {
            final String indyHome =
                System.getProperty( BootInterface.INDY_HOME_PROP, new File( "." ).getCanonicalPath() );

            return new BootOptions( bootDefaults, indyHome);
        }
        catch ( final IOException e )
        {
            throw new IndyBootException( "ERROR LOADING BOOT DEFAULTS: %s.\nReason: %s\n\n", e, bootDefaults,
                                          e.getMessage() );
        }
        catch ( final InterpolationException e )
        {
            throw new IndyBootException( "ERROR RESOLVING BOOT DEFAULTS: %s.\nReason: %s\n\n", e, bootDefaults,
                                          e.getMessage() );
        }
    }

    public void setSystemProperties()
    {
        final Properties properties = System.getProperties();

        System.out.printf( "\n\nUsing Indy configuration: %s\n", config );
        properties.setProperty( BootInterface.CONFIG_PATH_PROP, config );
        properties.setProperty( BootInterface.INDY_HOME_PROP, indyHome );
        properties.setProperty( BootInterface.APROX_CONFIG_PATH_PROP, config );
        properties.setProperty( BootInterface.APROX_HOME_PROP, indyHome );
        System.setProperties( properties );
    }

    public BootOptions()
    {

    }

    
    public BootOptions( final String indyHome )
            throws IOException, InterpolationException
        {
            this( null, indyHome );
        }
    

    public BootOptions( final File bootDefaults, final String indyHome )
        throws IOException, InterpolationException
    {
        this.indyHome = indyHome;
        this.bootProps = new Properties();
        
        if ( bootDefaults != null && bootDefaults.exists() )
        {
            FileInputStream stream = null;
            try
            {
                stream = new FileInputStream( bootDefaults );

                bootProps.load( stream );
            }
            finally
            {
                IOUtils.closeQuietly( stream );
            }
        }
        
        if ( bind == null )
        {
            bind = resolve( bootProps.getProperty( BIND_PROP, DEFAULT_BIND ) );
        }

        if ( port == null )
        {
            port = Integer.parseInt( resolve( bootProps.getProperty( PORT_PROP, Integer.toString( DEFAULT_PORT ) ) ) );
        }

        if ( config == null )
        {
            final String defaultConfigPath = new File( indyHome, "etc/indy/main.conf" ).getPath();
            config = resolve( bootProps.getProperty( CONFIG_PROP, defaultConfigPath ) );
        }

        contextPath = bootProps.getProperty( CONTEXT_PATH_PROP, contextPath );
    }

    public void copyFrom( final BootOptions options )
    {
        this.help = options.help;
        this.bind = options.bind;
        this.port = options.port;
        this.config = options.config;
        this.contextPath = options.contextPath;
        this.interp = options.interp;
        this.bootProps = options.bootProps;
        this.indyHome = options.indyHome;
    }

    public String resolve( final String value )
        throws InterpolationException
    {
        if ( value == null || value.trim()
                                   .length() < 1 )
        {
            return null;
        }

        if ( bootProps == null )
        {
            if ( indyHome == null )
            {
                return value;
            }
            else
            {
                bootProps = new Properties();
            }
        }

        bootProps.setProperty( "indy.home", indyHome );

        if ( interp == null )
        {
            interp = new StringSearchInterpolator();
            interp.addValueSource( new PropertiesBasedValueSource( bootProps ) );
        }

        return interp.interpolate( value );
    }

    public boolean isHelp()
    {
        return help;
    }

    public String getBind()
    {
        return bind;
    }

    public int getPort()
    {
        return port;
    }

    public String getConfig()
    {
        return config;
    }

    public BootOptions setHelp( final boolean help )
    {
        this.help = help;
        return this;
    }

    public BootOptions setBind( final String bind )
    {
        this.bind = bind;
        return this;
    }

    public BootOptions setPort( final int port )
    {
        this.port = port;
        return this;
    }

    public BootOptions setConfig( final String config )
    {
        this.config = config;
        return this;
    }

    public String getContextPath()
    {
        if ( contextPath == null )
        {
            contextPath = "";
        }

        if ( contextPath.startsWith( "/" ) )
        {
            contextPath = contextPath.substring( 1 );
        }

        return contextPath;
    }

    public void setContextPath( final String contextPath )
    {
        if ( contextPath == null )
        {
            this.contextPath = "";
        }
        else if ( contextPath.startsWith( "/" ) )
        {
            this.contextPath = contextPath.substring( 1 );
        }
        else
        {
            this.contextPath = contextPath;
        }
    }

    public boolean parseArgs( final String[] args )
        throws IndyBootException
    {
        final CmdLineParser parser = new CmdLineParser( this );
        boolean canStart = true;
        try
        {
            parser.parseArgument( args );
        }
        catch ( final CmdLineException e )
        {
            throw new IndyBootException( "Failed to parse command-line args: %s", e, e.getMessage() );
        }

        if ( isHelp() )
        {
            printUsage( parser, null );
            canStart = false;
        }

        return canStart;
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

    public String getIndyHome()
    {
        return indyHome;
    }

    public void setIndyHome( final String indyHome )
    {
        this.indyHome = indyHome;
    }

    public void setPort( final Integer port )
    {
        this.port = port;
    }

}
