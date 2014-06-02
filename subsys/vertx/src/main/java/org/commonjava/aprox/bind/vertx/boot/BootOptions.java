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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class BootOptions
{

    public static final String BIND_PROP = "bind";

    public static final String PORT_PROP = "port";

    public static final String CONFIG_PROP = "config";

    public static final String WORKERS_PROP = "workers";

    @Option( name = "-h", aliases = { "--help" }, usage = "Print this and exit" )
    private boolean help;

    @Option( name = "-i", aliases = { "--interface", "--bind", "--listen" }, usage = "Bind to a particular IP address (default: 0.0.0.0, or all available)" )
    private String bind = "0.0.0.0";

    @Option( name = "-p", aliases = { "--port" }, usage = "Use different port (default: 8080)" )
    private int port = 8080;

    @Option( name = "-c", aliases = { "--config" }, usage = "Use an alternative configuration file (default: /etc/aprox/main.conf)" )
    private String config;

    @Option( name = "-w", aliases = { "--workers" }, usage = "Number of worker threads to serve content (default: 5)" )
    private int workers = 5;

    private Weld weld;

    private WeldContainer container;

    private StringSearchInterpolator interp;

    private Properties bootProps;

    private String aproxHome;

    public BootOptions setDefaults( final File bootDefaults, final String aproxHome )
        throws IOException, InterpolationException
    {
        if ( bootDefaults != null && bootDefaults.exists() )
        {
            FileInputStream stream = null;
            try
            {
                stream = new FileInputStream( bootDefaults );

                this.aproxHome = aproxHome;

                bootProps = new Properties();
                bootProps.load( stream );

                bind = resolve( bootProps.getProperty( BIND_PROP, bind ) );
                port = Integer.parseInt( resolve( bootProps.getProperty( PORT_PROP, Integer.toString( port ) ) ) );
                workers =
                    Integer.parseInt( resolve( bootProps.getProperty( WORKERS_PROP, Integer.toString( workers ) ) ) );

                config = resolve( bootProps.getProperty( CONFIG_PROP, config ) );
            }
            finally
            {
                IOUtils.closeQuietly( stream );
            }
        }

        return this;
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
            if ( aproxHome == null )
            {
                return value;
            }
            else
            {
                bootProps = new Properties();
            }
        }

        bootProps.setProperty( "aprox.home", aproxHome );

        if ( interp == null )
        {
            interp = new StringSearchInterpolator();
            interp.addValueSource( new PropertiesBasedValueSource( bootProps ) );
        }

        return interp.interpolate( value );
    }

    public int getWorkers()
    {
        return workers;
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

    public BootOptions setWeldComponents( final Weld weld, final WeldContainer container )
    {
        this.weld = weld;
        this.container = container;
        return this;
    }

    public Weld getWeld()
    {
        return weld;
    }

    public WeldContainer getWeldContainer()
    {
        return container;
    }

    public BootOptions configureLogging( final File logConf )
        throws IOException, InterpolationException, JoranException
    {
        if ( logConf != null && logConf.exists() )
        {
            String logProps = FileUtils.readFileToString( logConf );
            logProps = resolve( logProps );

            final JoranConfigurator fig = new JoranConfigurator();
            final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

            final List<Logger> loggerList = context.getLoggerList();
            if ( loggerList != null )
            {
                for ( final Logger logger : loggerList )
                {
                    logger.detachAndStopAllAppenders();
                }
            }

            fig.setContext( context );
            fig.doConfigure( new ByteArrayInputStream( logProps.getBytes() ) );
        }

        return this;
    }

    public BootOptions setWorkers( final int workers )
    {
        this.workers = workers;
        return this;
    }

}
