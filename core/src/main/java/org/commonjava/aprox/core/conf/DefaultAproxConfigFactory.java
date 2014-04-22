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
package org.commonjava.aprox.core.conf;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.conf.AbstractAproxMapConfig;
import org.commonjava.aprox.conf.AproxConfigFactory;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.util.PathUtils;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.DefaultConfigurationListener;
import org.commonjava.web.config.dotconf.DotConfConfigurationReader;
import org.commonjava.web.config.io.ConfigFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultAproxConfigFactory
    extends DefaultConfigurationListener
    implements AproxConfigFactory
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<AproxConfigInfo> configSections;

    @Inject
    private Instance<AbstractAproxMapConfig> mapConfigs;

    //    private static String configPath = System.getProperty( CONFIG_PATH_PROP, DEFAULT_CONFIG_PATH );
    //
    //    public static void setConfigPath( final String path )
    //    {
    //        configPath = path;
    //    }
    //
    //    @PostConstruct
    @Override
    public synchronized void load( final String configPath )
        throws ConfigurationException
    {
        setSystemProperties();

        logger.info( "\n\n\n\n[CONFIG] Reading AProx configuration in: '{}'\n\nfrom:\n\n  {}\n\nAdding configuration section listeners:",
                     Thread.currentThread()
                           .getName(), StringUtils.join( new RuntimeException( "Diagnostic trace:" ).getStackTrace(), "\n  " ) );

        for ( final AproxConfigInfo section : configSections )
        {
            with( section.getSectionName(), section.getConfigurationClass() );
        }

        for ( final AbstractAproxMapConfig section : mapConfigs )
        {
            with( section.getSectionName(), section );
        }

        final String config = configPath( configPath );

        logger.info( "\n\n[CONFIG] Reading configuration in: '{}'\n\nfrom {}", Thread.currentThread()
                                                                                     .getName(), config );

        InputStream stream = null;
        try
        {
            stream = ConfigFileUtils.readFileWithIncludes( config );
            new DotConfConfigurationReader( this ).loadConfiguration( stream );
        }
        catch ( final IOException e )
        {
            throw new ConfigurationException( "Cannot open configuration file: {}. Reason: {}", e, configPath, e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }

        logger.info( "[CONFIG] AProx configuration complete for: '{}'.\n\n\n\n", Thread.currentThread()
                                                                                       .getName() );
    }

    private String configPath( final String configPath )
    {
        if ( configPath != null && !configPath.equals(""))
        {
            return configPath;
        }
        return System.getProperty( CONFIG_PATH_PROP );
    }

    private void setSystemProperties()
    {
        logger.info( "Verifying AProx system properties are set..." );
        /* Set config path */
        String confPath = System.getProperty( AproxConfigFactory.CONFIG_PATH_PROP );
        if ( confPath == null )
        {
            final String aproxHome = System.getProperty( "aprox.home" );
            if ( aproxHome != null )
            {
                final String path = PathUtils.join( aproxHome, "etc/aprox/main.conf" );
                if ( new File( path ).exists() )
                {
                    confPath = path;
                }
            }
        }
        if ( confPath == null )
        {
            confPath = AproxConfigFactory.DEFAULT_CONFIG_PATH;
        }
        System.setProperty( AproxConfigFactory.CONFIG_PATH_PROP, confPath );

        /* Set config dir */
        String confDir = System.getProperty( AproxConfigFactory.CONFIG_DIR_PROP);
        if ( confDir == null )
        {
            final File f = new File( confPath );
            final String dir = f.getParent();
            System.setProperty( AproxConfigFactory.CONFIG_DIR_PROP, dir );
        }
    }

}
