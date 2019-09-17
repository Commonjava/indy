/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.conf.IndyConfigFactory;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.conf.SystemPropertyProvider;
import org.commonjava.indy.util.PathUtils;
import org.commonjava.propulsor.config.ConfigUtils;
import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.ConfigurationListener;
import org.commonjava.propulsor.config.DefaultConfigurationListener;
import org.commonjava.propulsor.config.dotconf.DotConfConfigurationReader;
import org.commonjava.propulsor.config.io.ConfigFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.propulsor.config.ConfigUtils.loadStandardConfigProperties;

@ApplicationScoped
public class DefaultIndyConfigFactory
        extends DefaultConfigurationListener
    implements IndyConfigFactory
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<ConfigurationListener> configListeners;

    @Inject
    private Instance<IndyConfigInfo> configSections;

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
        logger.info( "\n\n\n\n[CONFIG] Reading Indy configuration in: '{}'\n\nAdding configuration section listeners:",
                     Thread.currentThread()
                           .getName() );

        logger.info( "Adding configuration sections..." );
        if ( configSections != null )
        {
            for ( final IndyConfigInfo section : configSections )
            {
                String sectionName = ConfigUtils.getSectionName( section.getClass() );
                logger.info( "Adding configuration section: {}", sectionName );
                with( sectionName, section );
            }
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

        List<ConfigurationListener> listeners = new ArrayList<>();
        listeners.add( this );

        if ( configListeners != null )
        {
            configListeners.forEach( (listener)->listeners.add( listener ) );
        }

        InputStream stream = null;
        try
        {
            Properties props = getBaseSystemProperties();

            configSections.forEach( (section)->{
                if ( section instanceof SystemPropertyProvider)
                {
                    Properties p = ( (SystemPropertyProvider) section ).getSystemPropertyAdditions();
                    p.stringPropertyNames().forEach( ( name ) -> props.setProperty( name, p.getProperty( name ) ) );
                }
            });

            stream = ConfigFileUtils.readFileWithIncludes( config );
            new DotConfConfigurationReader( listeners ).loadConfiguration( stream, props );

            System.setProperties( props );
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
        for ( final IndyConfigInfo section : configSections )
        {
            String sectionName = ConfigUtils.getSectionName( section.getClass() );
            logger.info( "Config section: {} with name: {}", section, sectionName );
            writeDefaultsFor( section, dir );
        }
    }

    private void writeDefaultsFor( final IndyConfigInfo section, final File dir )
        throws ConfigurationException
    {
        String sectionName = ConfigUtils.getSectionName( section.getClass() );
        logger.info( "Attempting to write default configuration for section: {}", sectionName );
        final InputStream configStream = section.getDefaultConfig();
        if ( configStream != null )
        {
            final String fname = section.getDefaultConfigFileName();
            if ( fname == null )
            {
                logger.info( "NOT writing default configuration for: {}. No defaults available.", sectionName );
                return;
            }

            final File file = new File( dir, fname );
            if ( !"main.conf".equals( fname ) && file.exists() )
            {
                logger.info( "NOT writing default configuration to: {}. That file already exists.", file );
                return;
            }

            file.getParentFile()
                .mkdirs();

            logger.info( "Writing defaults for: {} to: {}", sectionName, file );
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

    private Properties getBaseSystemProperties()
    {
        Properties props = loadStandardConfigProperties();

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

        props.setProperty( IndyConfigFactory.CONFIG_PATH_PROP, confPath );
        props.setProperty( IndyConfigFactory.APROX_CONFIG_PATH_PROP, confPath );

        /* Set config dir */
        final String confDir = System.getProperty( IndyConfigFactory.CONFIG_DIR_PROP );
        if ( confDir == null )
        {
            final File f = new File( confPath );
            final String dir = f.getParent();
            props.setProperty( IndyConfigFactory.CONFIG_DIR_PROP, dir );
            props.setProperty( IndyConfigFactory.APROX_CONFIG_DIR_PROP, dir );
        }

        return props;
    }

}
