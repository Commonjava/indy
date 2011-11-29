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
package org.commonjava.aprox.core.fixture;

import java.io.File;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.DefaultProxyConfiguration;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.test.fixture.TestData;

@Singleton
public class ProxyConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    public static final String APROX_DATABASE_URL = "aprox.db.url";

    private DefaultProxyConfiguration config;

    @Produces
    @TestData
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
    @TestData
    @Default
    public CouchDBConfiguration getCouchConfiguration()
    {
        System.out.println( "\n\n\n\n\nTEST CONFIG!!!!\n\n\n\n\n" );
        return getProxyConfiguration().getDatabaseConfig();
    }

}
