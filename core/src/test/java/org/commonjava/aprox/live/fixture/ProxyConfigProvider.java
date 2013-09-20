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
package org.commonjava.aprox.live.fixture;

import java.io.File;
import java.io.IOException;

import javax.annotation.PreDestroy;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.maven.galley.maven.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.defaults.StandardMaven304PluginDefaults;

@javax.enterprise.context.ApplicationScoped
public class ProxyConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    private DefaultAproxConfiguration config;

    private DefaultStorageProviderConfiguration storageConfig;

    private File dir;

    private MavenPluginDefaults pluginDefaults;

    @PreDestroy
    public synchronized void deleteRepoDir()
        throws IOException
    {
        FileUtils.forceDelete( dir );
    }

    @Produces
    @Default
    public synchronized MavenPluginDefaults getPluginDefaults()
    {
        if ( pluginDefaults == null )
        {
            pluginDefaults = new StandardMaven304PluginDefaults();
        }

        return pluginDefaults;
    }

    @Produces
    @Default
    public synchronized AproxConfiguration getProxyConfiguration()
    {
        if ( config == null )
        {
            config = new DefaultAproxConfiguration();
        }

        return config;
    }

    @Produces
    @Default
    public synchronized DefaultStorageProviderConfiguration getStorageProviderConfiguration()
        throws IOException
    {
        if ( storageConfig == null )
        {
            final String path = System.getProperty( REPO_ROOT_DIR );
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
