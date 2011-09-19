package org.commonjava.web.maven.proxy.change;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.commonjava.auth.couch.model.Permission;
import org.commonjava.web.maven.proxy.AbstractAProxLiveTest;
import org.commonjava.web.maven.proxy.model.Group;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class GroupDeletionListenerTest
    extends AbstractAProxLiveTest
{

    @Inject
    private GroupDeletionListener groupListener;

    @Test
    public void groupRolesRemovedWhenGroupDeleted()
        throws Exception
    {
        Group group = new Group( "test" );
        proxyManager.storeGroup( group );

        Permission perm =
            userManager.getPermission( Permission.name( Group.NAMESPACE, group.getName(),
                                                        Permission.ADMIN ) );
        assertThat( perm, notNullValue() );

        perm =
            userManager.getPermission( Permission.name( Group.NAMESPACE, group.getName(),
                                                        Permission.READ ) );
        assertThat( perm, notNullValue() );

        proxyManager.deleteGroup( group.getName() );

        System.out.println( "Waiting up to 20s for permission deletions to propagate..." );
        long start = System.currentTimeMillis();

        groupListener.waitForChange( 20000, 1000 );

        long elapsed = System.currentTimeMillis() - start;
        System.out.println( "Continuing test after " + elapsed + " ms." );

        perm =
            userManager.getPermission( Permission.name( Group.NAMESPACE, group.getName(),
                                                        Permission.ADMIN ) );
        assertThat( perm, nullValue() );

        perm =
            userManager.getPermission( Permission.name( Group.NAMESPACE, group.getName(),
                                                        Permission.READ ) );
        assertThat( perm, nullValue() );
    }

}
