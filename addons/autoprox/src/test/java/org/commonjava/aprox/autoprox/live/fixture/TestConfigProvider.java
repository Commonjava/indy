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
package org.commonjava.aprox.autoprox.live.fixture;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.autoprox.conf.AutoProxConfig;
import org.commonjava.aprox.autoprox.conf.AutoProxFactory;
import org.commonjava.aprox.autoprox.conf.FactoryMapping;
import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.web.json.test.WebFixture;

@javax.enterprise.context.ApplicationScoped
public class TestConfigProvider
{
    public static String REPO_ROOT_DIR = "repo.root.dir";

    private AutoProxConfig config;

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
    public synchronized AutoProxConfig getAutoProxConfig()
    {
        if ( config == null )
        {
            final AutoProxFactory fac = new TestAutoProxFactory( http );
            config = new AutoProxConfig( Collections.singletonList( new FactoryMapping( FactoryMapping.DEFAULT_MATCH, fac ) ), true );
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
