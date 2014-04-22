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
package org.commonjava.aprox.core.data;

public abstract class AbstractProxyDataManagerTCK
{

    protected abstract TCKFixtureProvider getFixtureProvider();

    // protected static ProxyDataManager manager;
    //
    // protected static ProxyConfiguration config;

    // protected static DefaultUserManagerConfig umConfig;

    // protected static CouchManager couch;

    // protected static PasswordManager passwordManager;
    //
    // protected static UserDataManager userManager;

    // @BeforeClass
    // public static void setupManager()
    // {
    // setupLogging( Level.DEBUG );
    //
    // config = new ProxyConfigProvider().getProxyConfiguration();
    //
    // // umConfig =
    // // new DefaultUserManagerConfig( "admin@nowhere.com", "password", "Admin", "User",
    // // "http://localhost:5984/test-aprox" );
    //
    // final Serializer serializer = new Serializer();
    //
    // couch =
    // new CouchManager( config.getDatabaseConfig(),
    // new CouchHttpClient( config.getDatabaseConfig(), serializer ), serializer,
    // new CouchAppReader() );
    //
    // // passwordManager = new PasswordManager();
    // // userManager = new UserDataManager( umConfig, passwordManager, couch );
    //
    // manager = new DefaultProxyDataManager( config, couch, serializer );
    // }

    // @Before
    // public void setupDB()
    // throws Exception
    // {
    // if ( couch.dbExists() )
    // {
    // couch.dropDatabase();
    // }
    // manager.install();
    // }
    //
    // @After
    // public void teardownDB()
    // throws Exception
    // {
    // couch.dropDatabase();
    // }

}
