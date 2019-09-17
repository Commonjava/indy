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
package org.commonjava.indy.fixture;

import java.io.File;
import java.io.IOException;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.indy.inject.TestData;

@javax.enterprise.context.ApplicationScoped
public class ProxyConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    private DefaultIndyConfiguration config;

    private DefaultStorageProviderConfiguration storageConfig;

    //    private MavenPluginDefaults pluginDefaults;
    //
    //    @Produces
    //    @TestData
    //    @Default
    //    public synchronized MavenPluginDefaults getPluginDefaults()
    //    {
    //        if ( pluginDefaults == null )
    //        {
    //            pluginDefaults = new StandardMaven304PluginDefaults();
    //        }
    //
    //        return pluginDefaults;
    //    }

    @Produces
    @TestData
    @Default
    public synchronized IndyConfiguration getProxyConfiguration()
    {
        if ( config == null )
        {
            config = new DefaultIndyConfiguration();
        }

        return config;
    }

    @Produces
    @TestData
    @Default
    public synchronized DefaultStorageProviderConfiguration getStorageProviderConfiguration()
        throws IOException
    {
        if ( storageConfig == null )
        {
            final String path = System.getProperty( REPO_ROOT_DIR );
            File dir;
            if ( path == null )
            {
                dir = File.createTempFile( "repo.root", ".dir" );
                dir.delete();
                dir.mkdirs();
            }
            else
            {
                dir = new File( path );
            }
            storageConfig = new DefaultStorageProviderConfiguration( dir );
        }

        return storageConfig;
    }

}
