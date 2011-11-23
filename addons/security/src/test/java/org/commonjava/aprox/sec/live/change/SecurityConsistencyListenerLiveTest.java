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
package org.commonjava.aprox.sec.live.change;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;

import javax.inject.Inject;

import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.aprox.sec.change.SecurityConsistencyListener;
import org.commonjava.aprox.sec.fixture.ProxyConfigProvider;
import org.commonjava.aprox.sec.live.AbstractAProxSecLiveTest;
import org.commonjava.auth.couch.model.Permission;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class SecurityConsistencyListenerLiveTest
    extends AbstractAProxSecLiveTest
{

    @Deployment
    public static WebArchive createWar()
    {
        return new TestWarArchiveBuilder( SecurityConsistencyListenerLiveTest.class ).withExtraClasses( AbstractAProxSecLiveTest.class,
                                                                                                        ProxyConfigProvider.class )
                                                                                     .withLibrariesIn( new File(
                                                                                                                 "target/dependency" ) )
                                                                                     .withLog4jProperties()
                                                                                     .build();
    }

    @Inject
    private SecurityConsistencyListener listener;

    @Test
    public void groupRolesRemovedWhenGroupDeleted()
        throws Exception
    {
        final Group group = new Group( "test" );

        System.out.println( "Storing test group..." );
        proxyManager.storeGroup( group );
        System.out.println( "...done." );

        System.out.println( "Verifying that permissions were created for test group..." );
        Permission perm =
            userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(), Permission.ADMIN ) );
        assertThat( perm, notNullValue() );

        perm = userManager.getPermission( Permission.name( StoreType.group.name(), group.getName(), Permission.READ ) );
        assertThat( perm, notNullValue() );
        System.out.println( "...done." );

        System.out.println( "Deleting test group..." );
        proxyManager.deleteGroup( group.getName() );
        System.out.println( "...done." );

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
