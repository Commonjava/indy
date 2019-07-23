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

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@ApplicationScoped
@SectionName( IndySchedulerConfig.SECTION_NAME )
public class IndySchedulerConfig
        extends MapSectionListener
        implements IndyConfigInfo
{

    //TODO: All quartz related function for content expiration has been
    //      replaced by ISPN cache timeout way. Will be removed in future

    public static final String SECTION_NAME = "scheduler";

    private static final String ENABLED_PROP = "enabled";

    private static final String CLUSTER_LOCK_EXPIRATION = "schedule.cluster.lock.expiration.sec";

//    @Deprecated
//    private static final String QUARTZ_DATASOURCE_PREFIX = "org.quartz.dataSource.ds.";
//
//    @Deprecated
//    private static final String DS_DRIVER = "driver";
//
//    @Deprecated
//    private static final String DS_URL = "URL";
//
//    @Deprecated
//    private static final String DDL_PROP = "ddl";
//
//    @Deprecated
//    private static final String DEFAULT_DB_URL =
//        String.format( "jdbc:derby:%s/var/lib/indy/data/scheduler",
//                       System.getProperty( "indy.home", System.getProperty( "java.io.tmpdir" ) ) );
//
//    @Deprecated
//    private static final String DEFAULT_DB_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

    private static final boolean DEFAULT_ENABLED = true;

    private static final int DEFAULT_CLUSTER_LOCK_EXPIRATION = 3600;

    private Boolean enabled;

    private Integer clusterLockExpiration;

//    @Deprecated
//    private transient boolean dbDetailsParsed;
//
//    @Deprecated
//    private transient String ddlFile;
//
//    @Deprecated
//    private transient String dbUrl;
//
//    @Deprecated
//    private transient String dbDriver;

    public IndySchedulerConfig()
    {
    }

    public IndySchedulerConfig( final Properties props )
            throws ConfigurationException
    {
        this.sectionStarted( SECTION_NAME );
        for ( final String key : props.stringPropertyNames() )
        {
            this.parameter( key, props.getProperty( key ) );
        }
    }

//    @Deprecated
//    private synchronized void parseDatabaseDetails()
//    {
//        if ( !dbDetailsParsed )
//        {
//            dbDetailsParsed = true;
//
//            final Map<String, String> configMap = getConfiguration();
//            if ( configMap == null )
//            {
//                return;
//            }
//
//            for ( final Map.Entry<String, String> entry : configMap.entrySet() )
//            {
//                final String key = entry.getKey();
//                if ( ENABLED_PROP.equalsIgnoreCase( key ) )
//                {
//                    enabled = Boolean.parseBoolean( entry.getValue().toLowerCase() );
//                }
//                else if ( DDL_PROP.equalsIgnoreCase( key ) )
//                {
//                    ddlFile = entry.getValue();
//                }
//                else if ( key.startsWith( QUARTZ_DATASOURCE_PREFIX ) )
//                {
//                    if ( key.endsWith( DS_DRIVER ) )
//                    {
//                        this.dbDriver = entry.getValue();
//                    }
//                    else if ( key.endsWith( DS_URL ) )
//                    {
//                        this.dbUrl = entry.getValue();
//                    }
//                }
//            }
//        }
//    }

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    public int getClusterLockExpiration()
    {
        return clusterLockExpiration == null ? DEFAULT_CLUSTER_LOCK_EXPIRATION : clusterLockExpiration;
    }

//    @Deprecated
//    public String getDdlFile()
//    {
//        parseDatabaseDetails();
//        return ddlFile;
//    }
//
//    @Deprecated
//    public String getDbUrl()
//    {
//        parseDatabaseDetails();
//        return dbUrl == null ? DEFAULT_DB_URL : dbUrl;
//    }
//
//    @Deprecated
//    public String getDbDriver()
//    {
//        parseDatabaseDetails();
//        return dbDriver == null ? DEFAULT_DB_DRIVER : dbDriver;
//    }

//    public CharSequence validate()
//    {
//                parseDatabaseDetails();
//                final StringBuilder sb = new StringBuilder();
//                if ( dbDriver == null )
//                {
//                    if ( sb.length() > 0 )
//                    {
//                        sb.append( "\n" );
//                    }
//                    sb.append( "Missing database driver (" )
//                      .append( QUARTZ_DATASOURCE_PREFIX )
//                      .append( DS_DRIVER )
//                      .append( ")" );
//                }
//
//                if ( dbUrl == null )
//                {
//                    if ( sb.length() > 0 )
//                    {
//                        sb.append( "\n" );
//                    }
//                    sb.append( "Missing database URL (" )
//                      .append( QUARTZ_DATASOURCE_PREFIX )
//                      .append( DS_URL )
//                      .append( ")" );
//                }
//
//                if ( sb.length() > 0 )
//                {
//                    return sb;
//                }
//
//        return null;
//    }

    @Override
    public Map<String, String> getConfiguration()
    {
        Map<String, String> configuration = super.getConfiguration();
        if ( configuration == null )
        {
            configuration = new HashMap<>();
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
