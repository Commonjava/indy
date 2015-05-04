/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.core.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.junit.Before;
import org.junit.Test;

public abstract class RepositoryDataManagerTCK
    extends AbstractProxyDataManagerTCK
{

    private StoreDataManager manager;

    @Before
    public void setup()
        throws Exception
    {
        doSetup();
        seedRepositoriesForGroupTests();
    }

    protected void doSetup()
        throws Exception
    {
    }

    protected void seedRepositoriesForGroupTests()
        throws Exception
    {
        manager = getFixtureProvider().getDataManager();
    }

    @Test
    public void createAndRetrieveCentralRepoProxy()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo, false );

        final RemoteRepository result = manager.getRemoteRepository( repo.getName() );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.getUrl(), equalTo( repo.getUrl() ) );
        assertThat( result.getUser(), nullValue() );
        assertThat( result.getPassword(), nullValue() );
    }

    @Test
    public void createCentralRepoProxyTwiceAndRetrieveOne()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo, true );

        storeRemoteRepository( repo, true );

        final List<? extends RemoteRepository> result = manager.getAllRemoteRepositories();

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 1 ) );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByName()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo, false );

        manager.deleteRemoteRepository( repo.getName(), summary );

        final ArtifactStore result = manager.getRemoteRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByObject()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo, false );

        manager.deleteRemoteRepository( repo, summary );

        final ArtifactStore result = manager.getRemoteRepository( repo.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createTwoReposAndRetrieveAll()
        throws Exception
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        storeRemoteRepository( repo );

        final RemoteRepository repo2 = new RemoteRepository( "test", "http://www.google.com" );
        storeRemoteRepository( repo2 );

        final List<? extends RemoteRepository> repositories = manager.getAllRemoteRepositories();

        assertThat( repositories, notNullValue() );
        assertThat( repositories.size(), equalTo( 2 ) );

        Collections.sort( repositories, new Comparator<RemoteRepository>()
        {

            @Override
            public int compare( final RemoteRepository r1, final RemoteRepository r2 )
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

    private void storeRemoteRepository( final RemoteRepository repo )
        throws Exception
    {
        manager.storeArtifactStore( repo, summary );
    }

    private void storeRemoteRepository( final RemoteRepository repo, final boolean skipIfExists )
        throws Exception
    {
        manager.storeArtifactStore( repo, summary, skipIfExists );
    }

}
