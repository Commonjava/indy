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
package org.commonjava.aprox.core.fixture;

import java.io.File;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.aprox.core.conf.DefaultProxyConfiguration;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.web.test.fixture.TestData;

@Singleton
public class ProxyConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    private static final String DB_URL = "http://localhost:5984/test-aprox";

    private DefaultProxyConfiguration config;

    @Produces
    @TestData
    public ProxyConfiguration getProxyConfiguration()
    {
        if ( config == null )
        {
            config = new DefaultProxyConfiguration( DB_URL );

            config.setRepositoryRootDirectory( new File( System.getProperty( REPO_ROOT_DIR ),
                                                         "target/repo-downloads" ) );
        }

        return config;
    }

    @Produces
    @AproxData
    @TestData
    @Default
    public CouchDBConfiguration getCouchConfiguration()
    {
        return getProxyConfiguration().getDatabaseConfig();
    }

}
