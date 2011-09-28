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
package org.commonjava.web.maven.proxy.data;

import static org.commonjava.couch.test.fixture.LoggingFixture.setupLogging;

import org.apache.log4j.Level;
import org.commonjava.auth.couch.conf.DefaultUserManagerConfig;
import org.commonjava.auth.couch.data.PasswordManager;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.conf.DefaultCouchDBConfiguration;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.io.CouchAppReader;
import org.commonjava.couch.io.CouchHttpClient;
import org.commonjava.couch.io.Serializer;
import org.commonjava.web.maven.proxy.conf.DefaultProxyConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;

public class AbstractProxyDataManagerTest
{
    protected static ProxyDataManager manager;

    protected static DefaultProxyConfiguration config;

    protected static DefaultUserManagerConfig umConfig;

    protected static CouchManager couch;

    protected static PasswordManager passwordManager;

    protected static UserDataManager userManager;

    @BeforeClass
    public static void setupManager()
    {
        setupLogging( Level.DEBUG );

        config = new DefaultProxyConfiguration();

        umConfig = new DefaultUserManagerConfig( "admin@nowhere.com", "password", "Admin", "User" );

        CouchDBConfiguration couchConfig =
            new DefaultCouchDBConfiguration( "http://localhost:5984/test-aprox" );

        Serializer serializer = new Serializer();

        couch =
            new CouchManager( couchConfig, new CouchHttpClient( couchConfig, serializer ),
                              serializer, new CouchAppReader() );

        passwordManager = new PasswordManager();
        userManager = new UserDataManager( umConfig, passwordManager, couch );

        manager = new ProxyDataManager( config, userManager, couchConfig, couch, serializer );
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

    // @After
    public void teardownDB()
        throws Exception
    {
        couch.dropDatabase();
    }

}
