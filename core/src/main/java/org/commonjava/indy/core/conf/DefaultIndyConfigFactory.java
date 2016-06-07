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
package org.commonjava.indy.core.conf;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.conf.AbstractIndyMapConfig;
import org.commonjava.indy.conf.IndyConfigClassInfo;
import org.commonjava.indy.conf.IndyConfigFactory;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.util.PathUtils;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.DefaultConfigurationListener;
import org.commonjava.web.config.dotconf.DotConfConfigurationReader;
import org.commonjava.web.config.io.ConfigFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultIndyConfigFactory
    extends DefaultConfigurationListener
    implements IndyConfigFactory
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<IndyConfigClassInfo> configSections;

    @Inject
    private Instance<AbstractIndyMapConfig> mapConfigs;

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

        logger.info( "\n\n\n\n[CONFIG] Reading Indy configuration in: '{}'\n\nAdding configuration section listeners:",
                     Thread.currentThread()
                           .getName() );

        for ( final IndyConfigClassInfo section : configSections )
        {
            logger.debug( "Adding class-oriented configuration listener: {}", section );

            with( section.getSectionName(), section.getConfigurationClass() );
        }

        for ( final AbstractIndyMapConfig section : mapConfigs )
        {
            logger.debug( "Adding map-oriented configuration listener: {}", section );

            with( section.getSectionName(), section );
        }

        final String config = configPath( configPath );

        logger.info( "\n\n[CONFIG] Reading configuration in: '{}'\n\nfrom {}", Thread.currentThread()
                                                                                     .getName(), config );

        File configFile = new File( config );
        if ( configFile.isDirectory() )
        {
            configFile = new File( configFile, "main.conf" );
        }

        if ( !configFile.exists() )
        {
            File dir = configFile;
            if ( dir.getName()
                    .equals( "main.conf" ) )
            {
                dir = dir.getParentFile();
            }

            logger.warn( "Cannot find configuration in: {}. Writing default configurations there for future modification.",
                         dir );

            if ( !dir.exists() && !dir.mkdirs() )
            {
                throw new ConfigurationException(
                                                  "Failed to create configuration directory: %s, in order to write defaults.",
                                                  dir );
            }

            writeDefaultConfigs( dir );
        }

        InputStream stream = null;
        try
        {
            stream = ConfigFileUtils.readFileWithIncludes( config );
            new DotConfConfigurationReader( this ).loadConfiguration( stream );
        }
        catch ( final IOException e )
        {
            throw new ConfigurationException( "Cannot open configuration file: {}. Reason: {}", e, configPath,
                                              e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }

        logger.info( "[CONFIG] Indy configuration complete for: '{}'.\n\n\n\n", Thread.currentThread()
                                                                                       .getName() );
    }

    @Override
    public void writeDefaultConfigs( final File dir )
        throws ConfigurationException
    {
        setSystemProperties();

        for ( final IndyConfigClassInfo section : configSections )
        {
            writeDefaultsFor( section, dir );
        }

        for ( final AbstractIndyMapConfig section : mapConfigs )
        {
            writeDefaultsFor( section, dir );
        }
    }

    private void writeDefaultsFor( final IndyConfigInfo section, final File dir )
        throws ConfigurationException
    {
        final InputStream configStream = section.getDefaultConfig();
        if ( configStream != null )
        {
            final String fname = section.getDefaultConfigFileName();
            final File file = new File( dir, fname );
            if ( !"main.conf".equals( fname ) && file.exists() )
            {
                logger.info( "NOT writing default configuration to: {}. That file already exists.", file );
                return;
            }

            file.getParentFile()
                .mkdirs();

            logger.info( "Writing defaults for: {} to: {}", section.getSectionName(), file );
            FileOutputStream out = null;
            try
            {
                // if the filename is 'main.conf', then APPEND the config.
                out = new FileOutputStream( file, fname.equals( "main.conf" ) );
                IOUtils.copy( configStream, out );
            }
            catch ( final IOException e )
            {
                throw new ConfigurationException( "Failed to write default configuration to: %s. Reason: %s", e, file,
                                                  e.getMessage() );
            }
            finally
            {
                closeQuietly( out );
                closeQuietly( configStream );
            }
        }
    }

    private String configPath( final String configPath )
    {
        if ( configPath != null && !configPath.equals( "" ) )
        {
            return configPath;
        }
        return System.getProperty( CONFIG_PATH_PROP );
    }

    private void setSystemProperties()
    {
        /* Set config path */
        String confPath = System.getProperty( IndyConfigFactory.CONFIG_PATH_PROP );
        final String indyHome = System.getProperty( "indy.home" );
        if ( confPath == null )
        {
            if ( indyHome != null )
            {
                final String path = PathUtils.join( indyHome, "etc/indy/main.conf" );
                if ( new File( path ).exists() )
                {
                    confPath = path;
                }
            }
        }

        if ( confPath == null )
        {
            confPath = IndyConfigFactory.DEFAULT_CONFIG_PATH;
        }

        System.setProperty( IndyConfigFactory.CONFIG_PATH_PROP, confPath );
        System.setProperty( IndyConfigFactory.APROX_CONFIG_PATH_PROP, confPath );

        /* Set config dir */
        final String confDir = System.getProperty( IndyConfigFactory.CONFIG_DIR_PROP );
        if ( confDir == null )
        {
            final File f = new File( confPath );
            final String dir = f.getParent();
            System.setProperty( IndyConfigFactory.CONFIG_DIR_PROP, dir );
            System.setProperty( IndyConfigFactory.APROX_CONFIG_DIR_PROP, dir );
        }
    }

}
