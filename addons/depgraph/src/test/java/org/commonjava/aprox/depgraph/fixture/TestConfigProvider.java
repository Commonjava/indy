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
package org.commonjava.aprox.depgraph.fixture;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;

@javax.enterprise.context.ApplicationScoped
public class TestConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    private AproxConfiguration proxyConfig;

    private DefaultStorageProviderConfiguration storageConfig;

    private AproxDepgraphConfig config;

    private FlatFileConfiguration dbConfig;

    private File dbDir;

    @PostConstruct
    public void start()
        throws IOException
    {
        dbDir = File.createTempFile( "depgraph.live.", ".dir" );
        dbDir.delete();
        dbDir.mkdirs();
    }

    @Produces
    @Default
    @TestData
    public synchronized FlatFileConfiguration getFlatFileConfig()
        throws IOException
    {
        if ( dbConfig == null )
        {
            dbConfig = new FlatFileConfiguration( dbDir );
        }

        return dbConfig;
    }

    @Produces
    @Default
    @TestData
    public synchronized AproxDepgraphConfig getDepgraphConfig()
    {
        if ( config == null )
        {
            config = new AproxDepgraphConfig();
        }

        return config;
    }

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
