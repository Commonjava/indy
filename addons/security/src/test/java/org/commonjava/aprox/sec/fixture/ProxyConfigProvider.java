/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.sec.fixture;

import java.io.File;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.DefaultProxyConfiguration;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.auth.couch.conf.DefaultUserManagerConfig;
import org.commonjava.auth.couch.conf.UserManagerConfiguration;
import org.commonjava.auth.couch.inject.UserData;
import org.commonjava.couch.conf.CouchDBConfiguration;

@Singleton
public class ProxyConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    public static final String APROX_DATABASE_URL = "aprox.db.url";

    public static final String USER_DATABASE_URL = "user.db.url";

    private DefaultProxyConfiguration config;

    private UserManagerConfiguration umConfig;

    @Produces
    // @TestData
    @UserData
    @Default
    public synchronized CouchDBConfiguration getCouchDBConfiguration()
    {
        return getUserManagerConfiguration().getDatabaseConfig();
    }

    @Produces
    // @TestData
    @Default
    public synchronized UserManagerConfiguration getUserManagerConfiguration()
    {
        if ( umConfig == null )
        {
            umConfig =
                new DefaultUserManagerConfig( "admin@nowhere.com", "password", "Admin", "User",
                                              "http://localhost:5984/test-user-manager" );
        }

        return umConfig;
    }

    @Produces
    // @TestData
    @Default
    public synchronized ProxyConfiguration getProxyConfiguration()
    {
        if ( config == null )
        {
            config = new DefaultProxyConfiguration( "http://localhost:5984/test-aprox" );

            config.setRepositoryRootDirectory( new File( System.getProperty( REPO_ROOT_DIR, "target/repo-downloads" ) ) );
        }

        return config;
    }

    @Produces
    @AproxData
    // @TestData
    // @Default
    public CouchDBConfiguration getCouchConfiguration()
    {
        return getProxyConfiguration().getDatabaseConfig();
    }

    // @Produces
    // @TestData
    // @Default
    // public synchronized ProxyConfiguration getWeldProxyConfiguration()
    // {
    // return getProxyConfiguration();
    // }
    //
    // @Produces
    // @AproxData
    // @TestData
    // @Default
    // public CouchDBConfiguration getWeldCouchConfiguration()
    // {
    // return getProxyConfiguration().getDatabaseConfig();
    // }

}
