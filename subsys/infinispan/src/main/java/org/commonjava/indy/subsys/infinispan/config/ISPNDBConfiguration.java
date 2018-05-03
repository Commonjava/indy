/**
 * Copyright (C) 2013 Red Hat, Inc.
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

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.indy.conf.SystemPropertyProvider;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.Properties;

@SectionName( "infinispan-db" )
@ApplicationScoped
public class ISPNDBConfiguration
        implements IndyConfigInfo, SystemPropertyProvider
{

    private static final String INDY_CACHE_DB_URL = "indyCacheDburl";

    private static final String INDY_CACHE_DB_USER = "indyCacheDbuser";

    private static final String INDY_CACHE_DB_PASS = "indyCacheDbpass";

    private static final String DEFAULT_INDY_CACHE_DB_URL = "jdbc:postgresql://localhost/indy";

    private static final String DEFAULT_INDY_CACHE_DB_USER = "indy";

    private static final String DEFAULT_INDY_CACHE_DB_PASS = "indy";

    private String cacheDBUrl;

    private String cacheDBUser;

    private String cacheDBPassword;

    public ISPNDBConfiguration()
    {
    }

    public String getCacheDBUrl()
    {
        return cacheDBUrl == null ? DEFAULT_INDY_CACHE_DB_URL : cacheDBUrl;
    }

    @ConfigName( INDY_CACHE_DB_URL )
    public void setCacheDBUrl( String cacheDBUrl )
    {
        this.cacheDBUrl = cacheDBUrl;
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
    public Properties getSystemProperties()
    {
        Properties properties = new Properties();
        preparePropertyInSysEnv( properties, INDY_CACHE_DB_URL, getCacheDBUrl() );
        preparePropertyInSysEnv( properties, INDY_CACHE_DB_USER, getCacheDBUser() );
        preparePropertyInSysEnv( properties, INDY_CACHE_DB_PASS, getCacheDBPassword() );

        return properties;
    }

    private void preparePropertyInSysEnv( Properties props, String propName, String ifNotInSysEnv )
    {
        final String envVal = System.getenv( propName );
        if ( envVal != null )
        {
            props.setProperty( propName, envVal );
        }
        else
        {
            props.setProperty( propName, ifNotInSysEnv );
        }
    }
}
