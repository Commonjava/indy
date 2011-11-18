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
package org.commonjava.aprox.core.data;

import static org.commonjava.couch.test.fixture.LoggingFixture.setupLogging;

import org.apache.log4j.Level;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.fixture.ProxyConfigProvider;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.io.CouchAppReader;
import org.commonjava.couch.io.CouchHttpClient;
import org.commonjava.couch.io.Serializer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public class AbstractProxyDataManagerTest
{
    protected static ProxyDataManager manager;

    protected static ProxyConfiguration config;

    // protected static DefaultUserManagerConfig umConfig;

    protected static CouchManager couch;

    // protected static PasswordManager passwordManager;
    //
    // protected static UserDataManager userManager;

    @BeforeClass
    public static void setupManager()
    {
        setupLogging( Level.DEBUG );

        config = new ProxyConfigProvider().getProxyConfiguration();

        // umConfig =
        // new DefaultUserManagerConfig( "admin@nowhere.com", "password", "Admin", "User",
        // "http://localhost:5984/test-aprox" );

        final Serializer serializer = new Serializer();

        couch =
            new CouchManager( config.getDatabaseConfig(),
                              new CouchHttpClient( config.getDatabaseConfig(), serializer ), serializer,
                              new CouchAppReader() );

        // passwordManager = new PasswordManager();
        // userManager = new UserDataManager( umConfig, passwordManager, couch );

        manager = new DefaultProxyDataManager( config, couch, serializer );
    }

    @Before
    public void setupDB()
        throws Exception
    {
        if ( couch.dbExists() )
        {
            couch.dropDatabase();
        }
        manager.install();
    }

    @After
    public void teardownDB()
        throws Exception
    {
        couch.dropDatabase();
    }

}
