package org.commonjava.web.maven.proxy.change;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.commonjava.auth.couch.model.Permission;
import org.commonjava.web.maven.proxy.AbstractAProxLiveTest;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class RepositoryDeletionListenerTest
    extends AbstractAProxLiveTest
{

    @Inject
    private RepositoryDeletionListener repositoryListener;

    @Test
    public void repositoryRolesRemovedWhenRepositoryDeleted()
        throws Exception
    {
        Repository repo = new Repository( "test", "http://repo1.maven.apache.org/maven2/" );
        proxyManager.storeRepository( repo );

        Permission perm =
            userManager.getPermission( Permission.name( Repository.NAMESPACE, repo.getName(),
                                                        Permission.ADMIN ) );
        assertThat( perm, notNullValue() );

        perm =
            userManager.getPermission( Permission.name( Repository.NAMESPACE, repo.getName(),
                                                        Permission.READ ) );
        assertThat( perm, notNullValue() );

        proxyManager.deleteRepository( repo.getName() );

        System.out.println( "Waiting up to 20s for permission deletions to propagate..." );
        long start = System.currentTimeMillis();

        repositoryListener.waitForChange( 20000, 1000 );

        long elapsed = System.currentTimeMillis() - start;
        System.out.println( "Continuing test after " + elapsed + " ms." );

        perm =
            userManager.getPermission( Permission.name( Repository.NAMESPACE, repo.getName(),
                                                        Permission.ADMIN ) );
        assertThat( perm, nullValue() );

        perm =
            userManager.getPermission( Permission.name( Repository.NAMESPACE, repo.getName(),
                                                        Permission.READ ) );
        assertThat( perm, nullValue() );
    }

    @Test
    public void groupsContainingRepositoryModifiedWhenRepositoryDeleted()
        throws Exception
    {
        Repository repo = new Repository( "test", "http://repo1.maven.apache.org/maven2/" );
        proxyManager.storeRepository( repo );

        Group group = new Group( "testGroup", repo.getName() );
        proxyManager.storeGroup( group );

        assertThat( group.getConstituents(), notNullValue() );
        assertThat( group.getConstituents().size(), equalTo( 1 ) );
        assertThat( group.getConstituents().iterator().next(), equalTo( repo.getName() ) );

        Group check = proxyManager.getGroup( group.getName() );

        assertThat( check.getConstituents(), notNullValue() );
        assertThat( check.getConstituents().size(), equalTo( 1 ) );
        assertThat( check.getConstituents().iterator().next(), equalTo( repo.getName() ) );

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
