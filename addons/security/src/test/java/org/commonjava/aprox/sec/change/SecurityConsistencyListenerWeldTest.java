/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.sec.change;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.log4j.Level;
import org.commonjava.aprox.core.data.DefaultProxyDataManager;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.sec.change.SecurityConsistencyListener;
import org.commonjava.aprox.sec.fixture.AproxDataLiteral;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.auth.couch.inject.UserDataLiteral;
import org.commonjava.auth.couch.model.Permission;
import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.test.fixture.LoggingFixture;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Before;
import org.junit.Test;

public class SecurityConsistencyListenerWeldTest
{

    private SecurityConsistencyListener listener;

    private ProxyDataManager proxyManager;

    private UserDataManager userManager;

    private CouchManager proxyCouch;

    private CouchManager userCouch;

    private CouchChangeListener proxyListener;

    @Before
    public void setup()
        throws Exception
    {
        LoggingFixture.setupLogging( Level.DEBUG );
        WeldContainer weld = new Weld().initialize();

        listener = weld.instance().select( SecurityConsistencyListener.class ).get();
        proxyManager = weld.instance().select( DefaultProxyDataManager.class ).get();
        userManager = weld.instance().select( UserDataManager.class ).get();

        proxyCouch = weld.instance().select( CouchManager.class, new AproxDataLiteral() ).get();
        proxyListener =
            weld.instance().select( CouchChangeListener.class, new AproxDataLiteral() ).get();

        userCouch = weld.instance().select( CouchManager.class, new UserDataLiteral() ).get();

        proxyCouch.dropDatabase();
        proxyManager.install();
        proxyListener.startup( true );

        userCouch.dropDatabase();
        userManager.install();
    }

    public void teardown()
        throws Exception
    {
        proxyCouch.dropDatabase();
        proxyListener.shutdown();

        userCouch.dropDatabase();
    }

    @Test
    public void groupRolesRemovedWhenGroupDeleted()
        throws Exception
    {
        Group group = new Group( "test" );
        proxyManager.storeGroup( group );

        Permission perm =
            userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(),
                                                        Permission.ADMIN ) );
        assertThat( perm, notNullValue() );

        perm =
            userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(),
                                                        Permission.READ ) );
        assertThat( perm, notNullValue() );

        proxyManager.deleteGroup( group.getName() );

        System.out.println( "Waiting up to 20s for permission deletions to propagate..." );
        long start = System.currentTimeMillis();

        listener.waitForChange( 20000, 1000 );

        long elapsed = System.currentTimeMillis() - start;
        System.out.println( "Continuing test after " + elapsed + " ms." );

        perm =
            userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(),
                                                        Permission.ADMIN ) );
        assertThat( perm, nullValue() );

        perm =
            userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(),
                                                        Permission.READ ) );
        assertThat( perm, nullValue() );
    }

    @Test
    public void repositoryRolesRemovedWhenRepositoryDeleted()
        throws Exception
    {
        Repository repo = new Repository( "test", "http://repo1.maven.apache.org/maven2/" );
        proxyManager.storeRepository( repo );

        Permission perm =
            userManager.getPermission( Permission.name( StoreType.repository.name(),
                                                        repo.getName(), Permission.ADMIN ) );
        assertThat( perm, notNullValue() );

        perm =
            userManager.getPermission( Permission.name( StoreType.repository.name(),
                                                        repo.getName(), Permission.READ ) );
        assertThat( perm, notNullValue() );

        proxyManager.deleteRepository( repo.getName() );

        System.out.println( "Waiting up to 20s for permission deletions to propagate..." );
        long start = System.currentTimeMillis();

        listener.waitForChange( 20000, 1000 );

        long elapsed = System.currentTimeMillis() - start;
        System.out.println( "Continuing test after " + elapsed + " ms." );

        perm =
            userManager.getPermission( Permission.name( StoreType.repository.name(),
                                                        repo.getName(), Permission.ADMIN ) );
        assertThat( perm, nullValue() );

        perm =
            userManager.getPermission( Permission.name( StoreType.repository.name(),
                                                        repo.getName(), Permission.READ ) );
        assertThat( perm, nullValue() );
    }

}
