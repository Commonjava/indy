/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.sec.fixture;

import java.io.File;
import java.util.Properties;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.DefaultProxyConfiguration;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.test.fixture.TestData;

@Singleton
public class ProxyConfigProvider
{

    private DefaultProxyConfiguration config;

    @Inject
    @TestData
    private Properties testProperties;

    public ProxyConfigProvider( final Properties testProperties )
    {
        this.testProperties = testProperties;
    }

    ProxyConfigProvider()
    {
    }

    @Produces
    // @TestData
    @Default
    public synchronized ProxyConfiguration getProxyConfiguration()
    {
        if ( config == null )
        {
            config =
                new DefaultProxyConfiguration(
                                               testProperties.getProperty( AProxSecTestPropertiesProvider.APROX_DATABASE_URL ) );

            config.setRepositoryRootDirectory( new File(
                                                         testProperties.getProperty( AProxSecTestPropertiesProvider.REPO_ROOT_DIR ) ) );
        }

        return config;
    }

    @Produces
    @AproxData
    // @TestData
    @Default
    public CouchDBConfiguration getCouchConfiguration()
    {
        return getProxyConfiguration().getDatabaseConfig();
    }

    @Produces
    @TestData
    @Default
    public synchronized ProxyConfiguration getWeldProxyConfiguration()
    {
        return getProxyConfiguration();
    }

    @Produces
    @AproxData
    @TestData
    @Default
    public CouchDBConfiguration getWeldCouchConfiguration()
    {
        return getProxyConfiguration().getDatabaseConfig();
    }

}
