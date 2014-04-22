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

@javax.enterprise.context.ApplicationScoped
public class ProxyConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    private DefaultAproxConfiguration config;

    private DefaultStorageProviderConfiguration storageConfig;

    private File dir;

    //    private MavenPluginDefaults pluginDefaults;

    @PreDestroy
    public synchronized void deleteRepoDir()
        throws IOException
    {
        FileUtils.forceDelete( dir );
    }

    //    @Produces
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
