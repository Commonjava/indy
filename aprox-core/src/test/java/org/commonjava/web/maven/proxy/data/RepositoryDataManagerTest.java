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
package org.commonjava.web.maven.proxy.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.commonjava.web.maven.proxy.model.ArtifactStore;
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

        ArtifactStore result = manager.getRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByObject()
        throws ProxyDataException
    {
        Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo, false );

        manager.deleteRepository( repo );

        ArtifactStore result = manager.getRepository( repo.getName() );

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

        ArtifactStore r = repositories.get( 0 );
        assertThat( r.getName(), equalTo( repo.getName() ) );

        r = repositories.get( 1 );
        assertThat( r.getName(), equalTo( repo2.getName() ) );
    }

}
