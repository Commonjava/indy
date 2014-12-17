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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxMapConfig;
import org.commonjava.web.config.ConfigurationException;

@ApplicationScoped
@Named( AproxSchedulerConfig.SECTION_NAME )
public class AproxSchedulerConfig
    extends AbstractAproxMapConfig
{

    public static final String SECTION_NAME = "scheduler";

    private static final String QUARTZ_DATASOURCE_PREFIX = "org.quartz.dataSource.ds.";

    private static final String DS_DRIVER = "driver";

    private static final String DS_URL = "URL";

    private static final String DDL_PROP = "ddl";

    private static final String DEFAULT_DB_URL =
        String.format( "jdbc:derby:%s/var/lib/aprox/data/scheduler",
                       System.getProperty( "aprox.home", System.getProperty( "java.io.tmpdir" ) ) );

    private static final String DEFAULT_DB_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    private transient boolean dbDetailsParsed;

    private transient String ddlFile;

    private transient String dbUrl;

    private transient String dbDriver;

    public AproxSchedulerConfig()
    {
        super( SECTION_NAME );
    }

    public AproxSchedulerConfig( final Properties props )
        throws ConfigurationException
    {
        super( SECTION_NAME );
        this.sectionStarted( SECTION_NAME );
        for ( final String key : props.stringPropertyNames() )
        {
            this.parameter( key, props.getProperty( key ) );
        }
    }

    private synchronized void parseDatabaseDetails()
    {
        if ( !dbDetailsParsed )
        {
            dbDetailsParsed = true;

            final Map<String, String> configMap = getConfiguration();
            if ( configMap == null )
            {
                return;
            }

            for ( final Map.Entry<String, String> entry : configMap.entrySet() )
            {
                final String key = entry.getKey();
                if ( DDL_PROP.equalsIgnoreCase( key ) )
                {
                    ddlFile = entry.getValue();
                }
                else if ( key.startsWith( QUARTZ_DATASOURCE_PREFIX ) )
                {
                    if ( key.endsWith( DS_DRIVER ) )
                    {
                        this.dbDriver = entry.getValue();
                    }
                    else if ( key.endsWith( DS_URL ) )
                    {
                        this.dbUrl = entry.getValue();
                    }
                }
            }
        }
    }

    public String getDdlFile()
    {
        parseDatabaseDetails();
        return ddlFile;
    }

    public String getDbUrl()
    {
        parseDatabaseDetails();
        return dbUrl == null ? DEFAULT_DB_URL : dbUrl;
    }

    public String getDbDriver()
    {
        parseDatabaseDetails();
        return dbDriver == null ? DEFAULT_DB_DRIVER : dbDriver;
    }

    public CharSequence validate()
    {
        //        parseDatabaseDetails();
        //        final StringBuilder sb = new StringBuilder();
        //        if ( dbDriver == null )
        //        {
        //            if ( sb.length() > 0 )
        //            {
        //                sb.append( "\n" );
        //            }
        //            sb.append( "Missing database driver (" )
        //              .append( QUARTZ_DATASOURCE_PREFIX )
        //              .append( DS_DRIVER )
        //              .append( ")" );
        //        }
        //
        //        if ( dbUrl == null )
        //        {
        //            if ( sb.length() > 0 )
        //            {
        //                sb.append( "\n" );
        //            }
        //            sb.append( "Missing database URL (" )
        //              .append( QUARTZ_DATASOURCE_PREFIX )
        //              .append( DS_URL )
        //              .append( ")" );
        //        }
        //
        //        if ( sb.length() > 0 )
        //        {
        //            return sb;
        //        }

        return null;
    }

    @Override
    public Map<String, String> getConfiguration()
    {
        Map<String, String> configuration = super.getConfiguration();
        if ( configuration == null )
        {
            configuration = new HashMap<>();
        }

        if ( configuration.isEmpty() )
        {

        }

        return configuration;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/scheduler.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-scheduler.conf" );
    }

}
