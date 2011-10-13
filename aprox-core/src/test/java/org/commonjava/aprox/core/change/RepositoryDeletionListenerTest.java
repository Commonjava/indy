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
package org.commonjava.aprox.core.change;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.commonjava.aprox.core.AbstractAProxLiveTest;
import org.commonjava.aprox.core.change.StoreDeletionListener;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.auth.couch.model.Permission;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class RepositoryDeletionListenerTest
    extends AbstractAProxLiveTest
{

    @Inject
    private StoreDeletionListener repositoryListener;

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

        repositoryListener.waitForChange( 20000, 1000 );

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

    @Test
    public void groupsContainingRepositoryModifiedWhenRepositoryDeleted()
        throws Exception
    {
        Repository repo = new Repository( "test", "http://repo1.maven.apache.org/maven2/" );
        proxyManager.storeRepository( repo );

        Group group = new Group( "testGroup", repo.getKey() );
        proxyManager.storeGroup( group );

        assertThat( group.getConstituents(), notNullValue() );
        assertThat( group.getConstituents().size(), equalTo( 1 ) );
        assertThat( group.getConstituents().iterator().next(), equalTo( repo.getKey() ) );

        Group check = proxyManager.getGroup( group.getName() );

        assertThat( check.getConstituents(), notNullValue() );
        assertThat( check.getConstituents().size(), equalTo( 1 ) );
        assertThat( check.getConstituents().iterator().next(), equalTo( repo.getKey() ) );

        proxyManager.deleteRepository( repo.getName() );

        System.out.println( "Waiting up to 20s for deletions to propagate..." );
        long start = System.currentTimeMillis();

        repositoryListener.waitForChange( 20000, 1000 );

        long elapsed = System.currentTimeMillis() - start;
        System.out.println( "Continuing test after " + elapsed + " ms." );

        check = proxyManager.getGroup( group.getName() );
        boolean result = check.getConstituents() == null || check.getConstituents().isEmpty();

        assertThat( result, equalTo( true ) );
    }

}
