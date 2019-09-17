/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.core.data;

import org.commonjava.indy.audit.ChangeSummary;

public abstract class AbstractProxyDataManagerTCK
{

    protected ChangeSummary summary = new ChangeSummary( "test-user", "test" );

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
    // // "http://localhost:5984/test-indy" );
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
