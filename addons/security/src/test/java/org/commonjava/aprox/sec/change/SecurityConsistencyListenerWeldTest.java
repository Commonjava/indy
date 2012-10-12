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
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.badgr.data.BadgrDataManager;
import org.commonjava.badgr.model.Permission;
import org.commonjava.util.logging.Log4jUtil;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SecurityConsistencyListenerWeldTest
{

    private SecurityConsistencyListener listener;

    private StoreDataManager proxyManager;

    private BadgrDataManager userManager;

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Before
    public void setup()
        throws Exception
    {
        final WeldContainer weld = new Weld().initialize();

        listener = weld.instance()
                       .select( SecurityConsistencyListener.class )
                       .get();
        proxyManager = weld.instance()
                           .select( StoreDataManager.class )
                           .get();
        userManager = weld.instance()
                          .select( BadgrDataManager.class )
                          .get();

        proxyManager.install();
    }

    @Test
    public void groupRolesRemovedWhenGroupDeleted()
        throws Exception
    {
        final Group group = new Group( "test" );
        proxyManager.storeGroup( group );

        Permission perm =
            userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(), Permission.ADMIN ) );
        assertThat( perm, notNullValue() );

        perm = userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(), Permission.READ ) );
        assertThat( perm, notNullValue() );

        proxyManager.deleteGroup( group.getName() );

        System.out.println( "Waiting up to 20s for permission deletions to propagate..." );
        final long start = System.currentTimeMillis();

        listener.waitForChange( 20000, 1000 );

        final long elapsed = System.currentTimeMillis() - start;
        System.out.println( "Continuing test after " + elapsed + " ms." );

        perm = userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(), Permission.ADMIN ) );
        assertThat( perm, nullValue() );

        perm = userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(), Permission.READ ) );
        assertThat( perm, nullValue() );
    }

    @Test
    public void repositoryRolesRemovedWhenRepositoryDeleted()
        throws Exception
    {
        final Repository repo = new Repository( "test", "http://repo1.maven.apache.org/maven2/" );
        proxyManager.storeRepository( repo );

        Permission perm =
            userManager.getPermission( Permission.name( StoreType.repository.name(), repo.getName(), Permission.ADMIN ) );
        assertThat( perm, notNullValue() );

        perm =
            userManager.getPermission( Permission.name( StoreType.repository.name(), repo.getName(), Permission.READ ) );
        assertThat( perm, notNullValue() );

        proxyManager.deleteRepository( repo.getName() );

        System.out.println( "Waiting up to 20s for permission deletions to propagate..." );
        final long start = System.currentTimeMillis();

        listener.waitForChange( 20000, 1000 );

        final long elapsed = System.currentTimeMillis() - start;
        System.out.println( "Continuing test after " + elapsed + " ms." );

        perm =
            userManager.getPermission( Permission.name( StoreType.repository.name(), repo.getName(), Permission.ADMIN ) );
        assertThat( perm, nullValue() );

        perm =
            userManager.getPermission( Permission.name( StoreType.repository.name(), repo.getName(), Permission.READ ) );
        assertThat( perm, nullValue() );
    }

}
