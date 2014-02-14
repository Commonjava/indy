/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.autoprox.live.fixture;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.autoprox.conf.AutoProxConfiguration;
import org.commonjava.aprox.autoprox.conf.AutoProxModel;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.web.json.test.WebFixture;

@javax.enterprise.context.ApplicationScoped
public class TestConfigProvider
{
    public static String REPO_ROOT_DIR = "repo.root.dir";

    private AutoProxModel model;

    private AutoProxConfiguration config;

    private AproxConfiguration proxyConfig;

    private final WebFixture http = new WebFixture();

    private DefaultStorageProviderConfiguration storageConfig;

    //    private MavenPluginDefaults pluginDefaults;
    //
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
    public synchronized AproxConfiguration getProxyConfig()
    {
        if ( proxyConfig == null )
        {
            proxyConfig = new DefaultAproxConfiguration();
        }
        return proxyConfig;
    }

    @Produces
    @Default
    @TestData
    public synchronized AutoProxConfiguration getAutoProxConfiguration()
    {
        if ( config == null )
        {
            config = new AutoProxConfiguration();
            config.setEnabled( true );
            config.setDeployEnabled( true );
        }

        return config;
    }

    @Produces
    @Default
    public synchronized AutoProxModel getAutoProxModel()
        throws MalformedURLException
    {
        if ( model == null )
        {
            model = new AutoProxModel();
            model.setRemoteRepository( new RemoteRepository( "repo", http.resourceUrl( "target/${name}" ) ) );
            model.setGroup( new Group( "group", new StoreKey( StoreType.remote, "first" ), new StoreKey( StoreType.remote, "second" ) ) );

            System.out.println( "\n\n\n\nSet Autoprox URL: " + model.getRemoteRepository()
                                                                    .getUrl() + "\n\n\n\n" );
        }

        return model;
    }

    @Produces
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
