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
package org.commonjava.aprox.core.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.core.model.Repository;
import org.junit.Test;

public abstract class RepositoryDataManagerTCK
    extends AbstractProxyDataManagerTCK
{

    @Test
    public void createAndRetrieveCentralRepoProxy()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Repository repo = factory.createRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo, false );

        final Repository result = manager.getRepository( repo.getName() );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.getUrl(), equalTo( repo.getUrl() ) );
        assertThat( result.getUser(), nullValue() );
        assertThat( result.getPassword(), nullValue() );
    }

    @Test
    public void createCentralRepoProxyTwiceAndRetrieveOne()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Repository repo = factory.createRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo, true );

        manager.storeRepository( repo, true );

        final List<Repository> result = manager.getAllRepositories();

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 1 ) );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByName()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Repository repo = factory.createRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo, false );

        manager.deleteRepository( repo.getName() );

        final ArtifactStore result = manager.getRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByObject()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Repository repo = factory.createRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo, false );

        manager.deleteRepository( repo );

        final ArtifactStore result = manager.getRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createTwoReposAndRetrieveAll()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Repository repo = factory.createRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        manager.storeRepository( repo );

        final Repository repo2 = factory.createRepository( "test", "http://www.google.com" );
        manager.storeRepository( repo2 );

        final List<Repository> repositories = manager.getAllRepositories();

        assertThat( repositories, notNullValue() );
        assertThat( repositories.size(), equalTo( 2 ) );

        Collections.sort( repositories, new Comparator<Repository>()
        {

            @Override
            public int compare( final Repository r1, final Repository r2 )
            {
                return r1.getName()
                         .compareTo( r2.getName() );
            }
        } );

        ArtifactStore r = repositories.get( 0 );
        assertThat( r.getName(), equalTo( repo.getName() ) );

        r = repositories.get( 1 );
        assertThat( r.getName(), equalTo( repo2.getName() ) );
    }

}
