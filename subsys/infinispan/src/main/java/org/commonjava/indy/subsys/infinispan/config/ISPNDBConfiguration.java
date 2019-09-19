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
package org.commonjava.indy.subsys.infinispan.config;

import org.apache.commons.lang.StringUtils;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.conf.SystemPropertyProvider;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.Properties;

@SectionName( "infinispan-db" )
@ApplicationScoped
public class ISPNDBConfiguration
        implements IndyConfigInfo, SystemPropertyProvider
{

    private static final String INDY_CACHE_DB_NAME = "datasource_name";

    private static final String INDY_CACHE_DB_SERVER = "datasource_server";

    private static final String INDY_CACHE_DB_PORT = "datasource_port";

    private static final String INDY_CACHE_DB_USER = "datasource_user";

    private static final String INDY_CACHE_DB_PASS = "datasource_password";

    private static final String DEFAULT_INDY_CACHE_DB_SERVER = "localhost";

    private static final String DEFAULT_INDY_CACHE_DB_PORT = "5432";

    private static final String DEFAULT_INDY_CACHE_DB_NAME = "indy";

    private static final String DEFAULT_INDY_CACHE_DB_USER = "indy";

    private static final String DEFAULT_INDY_CACHE_DB_PASS = "indy";

    private String cacheDbServer;

    private String cacheDbPort;

    private String cacheDbName;

    private String cacheDBUser;

    private String cacheDBPassword;

    public ISPNDBConfiguration()
    {
    }

    public String getCacheDbServer()
    {
        return cacheDbServer == null ? DEFAULT_INDY_CACHE_DB_SERVER : cacheDbServer;
    }

    @ConfigName( INDY_CACHE_DB_SERVER )
    public void setCacheDbServer( String cacheDbServer )
    {
        this.cacheDbServer = cacheDbServer;
    }

    public String getCacheDbPort()
    {
        return cacheDbPort == null ? DEFAULT_INDY_CACHE_DB_PORT : cacheDbPort;
    }

    @ConfigName( INDY_CACHE_DB_PORT )
    public void setCacheDbPort( String cacheDbPort )
    {
        this.cacheDbPort = cacheDbPort;
    }

    public String getCacheDbName()
    {
        return cacheDbName == null ? DEFAULT_INDY_CACHE_DB_NAME : cacheDbName;
    }

    @ConfigName( INDY_CACHE_DB_NAME )
    public void setCacheDbName( String cacheDbName )
    {
        this.cacheDbName = cacheDbName;
    }

    public String getCacheDBUser()
    {
        return cacheDBUser == null ? DEFAULT_INDY_CACHE_DB_USER : cacheDBUser;
    }

    @ConfigName( INDY_CACHE_DB_USER )
    public void setCacheDBUser( String cacheDBUser )
    {
        this.cacheDBUser = cacheDBUser;
    }

    public String getCacheDBPassword()
    {
        return cacheDBPassword == null ? DEFAULT_INDY_CACHE_DB_PASS : cacheDBPassword;
    }

    @ConfigName( INDY_CACHE_DB_PASS )
    public void setCacheDBPassword( String cacheDBPassword )
    {
        this.cacheDBPassword = cacheDBPassword;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return IndyConfigInfo.APPEND_DEFAULTS_TO_MAIN_CONF;
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-ispndb.conf" );
    }

    @Override
    public Properties getSystemPropertyAdditions()
    {
        Properties properties = new Properties();
        preparePropertyInSysEnv( properties, INDY_CACHE_DB_SERVER, getCacheDbServer() );
        preparePropertyInSysEnv( properties, INDY_CACHE_DB_PORT, getCacheDbPort() );
        preparePropertyInSysEnv( properties, INDY_CACHE_DB_NAME, getCacheDbName() );
        preparePropertyInSysEnv( properties, INDY_CACHE_DB_USER, getCacheDBUser() );
        preparePropertyInSysEnv( properties, INDY_CACHE_DB_PASS, getCacheDBPassword() );

        return properties;
    }

    private void preparePropertyInSysEnv( Properties props, String propName, String ifNotInSysEnv )
    {
        String propVal = System.getenv( propName );
        if ( StringUtils.isBlank( propVal ) )
        {
            propVal = System.getProperty( propName );
        }
        if ( StringUtils.isBlank( propVal ) )
        {
            propVal = ifNotInSysEnv;
        }

        propVal = propVal.replace( "\"", "&quot;" )
                         .replace( "'", "&apos;" )
                         .replace( "<", "&lt;" )
                         .replace( ">", "&gt;" );

        props.setProperty( propName, propVal );
    }
}
