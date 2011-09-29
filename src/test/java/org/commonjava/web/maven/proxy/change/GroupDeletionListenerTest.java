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
package org.commonjava.web.maven.proxy.change;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.commonjava.auth.couch.model.Permission;
import org.commonjava.web.maven.proxy.AbstractAProxLiveTest;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.StoreType;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class GroupDeletionListenerTest
    extends AbstractAProxLiveTest
{

    @Inject
    private StoreDeletionListener groupListener;

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

        groupListener.waitForChange( 20000, 1000 );

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

}
