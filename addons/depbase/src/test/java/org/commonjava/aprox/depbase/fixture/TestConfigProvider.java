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
package org.commonjava.aprox.depbase.fixture;

import java.io.File;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.core.conf.AproxConfiguration;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.conf.DefaultCouchDBConfiguration;
import org.commonjava.depbase.inject.DepbaseData;

@Singleton
public class TestConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    private DefaultAproxConfiguration config;

    private DefaultCouchDBConfiguration depbaseConfig;

    private DefaultCouchDBConfiguration aproxConfig;

    @Produces
    @DepbaseData
    @Default
    public synchronized CouchDBConfiguration getDepBaseDBConfiguration()
    {
        if ( depbaseConfig == null )
        {
            depbaseConfig = new DefaultCouchDBConfiguration( "http://localhost:5984/test-depbase" );
        }
        return depbaseConfig;
    }

    @Produces
    @AproxData
    @Default
    public synchronized CouchDBConfiguration getAproxDBConfiguration()
    {
        if ( aproxConfig == null )
        {
            aproxConfig = new DefaultCouchDBConfiguration( "http://localhost:5984/test-aprox" );
        }
        return aproxConfig;
    }

    @Produces
    @Default
    public synchronized AproxConfiguration getProxyConfiguration()
    {
        if ( config == null )
        {
            config =
                new DefaultAproxConfiguration( new File( System.getProperty( REPO_ROOT_DIR, "target/repo-downloads" ) ) );
        }

        return config;
    }

}
