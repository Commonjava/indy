package org.commonjava.web.maven.proxy.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.commonjava.web.maven.proxy.model.Repository;
import org.junit.Test;

public class RepositoryDataManagerTest
    extends AbstractProxyDataManagerTest
{

    @Test
    public void createAndRetrieveCentralRepoProxy()
        throws ProxyDataException
    {
        Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo, false );

        Repository result = manager.getRepository( repo.getName() );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.getUrl(), equalTo( repo.getUrl() ) );
        assertThat( result.getUser(), nullValue() );
        assertThat( result.getPassword(), nullValue() );
    }

    @Test
    public void createCentralRepoProxyTwiceAndRetrieveOne()
        throws ProxyDataException
    {
        Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo, true );

        manager.storeRepository( repo, true );

        List<Repository> result = manager.getAllRepositories();

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 1 ) );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByName()
        throws ProxyDataException
    {
        Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo, false );

        manager.deleteRepository( repo.getName() );

        Repository result = manager.getRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByObject()
        throws ProxyDataException
    {
        Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo, false );

        manager.deleteRepository( repo );

        Repository result = manager.getRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createTwoReposAndRetrieveAll()
        throws ProxyDataException
    {
        Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo );

        Repository repo2 = new Repository( "test", "http://www.google.com" );
        manager.storeRepository( repo2 );

        List<Repository> repositories = manager.getAllRepositories();

        assertThat( repositories, notNullValue() );
        assertThat( repositories.size(), equalTo( 2 ) );

        Collections.sort( repositories, new Comparator<Repository>()
        {

            @Override
            public int compare( final Repository r1, final Repository r2 )
            {
                return r1.getName().compareTo( r2.getName() );
            }
        } );

        Repository r = repositories.get( 0 );
        assertThat( r.getName(), equalTo( repo.getName() ) );

        r = repositories.get( 1 );
        assertThat( r.getName(), equalTo( repo2.getName() ) );
    }

}
