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

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;

public abstract class GroupDataManagerTCK
    extends AbstractProxyDataManagerTCK
{

    @Before
    public void seedRepositoriesForGroupTests()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        manager.storeRepository( factory.createRepository( "central", "http://repo1.maven.apache.org/maven2/" ) );
        manager.storeRepository( factory.createRepository( "repo2", "http://repo1.maven.org/maven2/" ) );
    }

    @Test
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void createAndRetrieveEmptyGroup()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Group grp = factory.createGroup( "test" );

        manager.storeGroup( grp );

        final Group result = manager.getGroup( grp.getName() );

        assertThat( result, notNullValue() );
        assertThat( result.getName(), equalTo( grp.getName() ) );
        assertThat( result.getConstituents(), anyOf( nullValue(), new BaseMatcher<List>()
        {
            @Override
            public boolean matches( final Object item )
            {
                return ( item instanceof List ) && ( (List) item ).isEmpty();
            }

            @Override
            public void describeTo( final Description description )
            {
                description.appendText( "empty list" );
            }
        } ) );
    }

    @Test
    public void createAndDeleteGroup_ByName()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Group grp = factory.createGroup( "test" );

        manager.storeGroup( grp );

        manager.deleteGroup( grp.getName() );

        final Group result = manager.getGroup( grp.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndDeleteGroup_ByObject()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Group grp = factory.createGroup( "test" );

        manager.storeGroup( grp );

        manager.deleteGroup( grp );

        final Group result = manager.getGroup( grp.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndRetrieveGroupWithTwoConstituents()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Group grp =
            factory.createGroup( "test", new StoreKey( StoreType.repository, "central" ),
                                 new StoreKey( StoreType.repository, "repo2" ) );

        manager.storeGroup( grp );

        final Group result = manager.getGroup( grp.getName() );

        assertThat( result, notNullValue() );
        assertThat( result.getName(), equalTo( grp.getName() ) );

        final List<StoreKey> repos = result.getConstituents();
        assertThat( repos, notNullValue() );
        assertThat( repos.size(), equalTo( 2 ) );

        assertThat( repos.get( 0 ), equalTo( new StoreKey( StoreType.repository, "central" ) ) );
        assertThat( repos.get( 1 ), equalTo( new StoreKey( StoreType.repository, "repo2" ) ) );
    }

    @Test
    public void createGroupAndRetrieveReposForThatGroupInOrder()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Group grp =
            factory.createGroup( "test", new StoreKey( StoreType.repository, "repo2" ),
                                 new StoreKey( StoreType.repository, "central" ) );

        manager.storeGroup( grp );

        final List<ArtifactStore> repos = manager.getOrderedConcreteStoresInGroup( grp.getName() );

        assertThat( repos, notNullValue() );
        assertThat( repos.size(), equalTo( 2 ) );

        assertThat( repos.get( 0 )
                         .getName(), equalTo( "repo2" ) );
        assertThat( repos.get( 1 )
                         .getName(), equalTo( "central" ) );
    }

    @Test
    public void createGroupAndRetrieveRepositoryConstituents()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Group grp =
            factory.createGroup( "test", new StoreKey( StoreType.repository, "central" ),
                                 new StoreKey( StoreType.repository, "repo2" ) );

        manager.storeGroup( grp );

        final List<ArtifactStore> result = manager.getOrderedConcreteStoresInGroup( grp.getName() );

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 2 ) );

        ArtifactStore repo = result.get( 0 );
        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "central" ) );

        repo = result.get( 1 );
        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "repo2" ) );
    }

    @Test
    public void createSameGroupTwiceAndRetrieveOne()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Group grp = factory.createGroup( "test" );

        manager.storeGroup( grp, true );
        manager.storeGroup( grp, true );

        final List<Group> result = manager.getAllGroups();

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 1 ) );
    }

    @Test
    public void createTwoGroupsAndRetrieveBoth()
        throws ProxyDataException
    {
        final ProxyDataManager manager = getFixtureProvider().getDataManager();
        final ModelFactory factory = getFixtureProvider().getModelFactory();

        final Group grp = factory.createGroup( "test" );
        final Group grp2 = factory.createGroup( "test2" );

        manager.storeGroup( grp );
        manager.storeGroup( grp2 );

        final List<Group> result = manager.getAllGroups();

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 2 ) );

        Collections.sort( result, new Comparator<Group>()
        {
            @Override
            public int compare( final Group g1, final Group g2 )
            {
                return g1.getName()
                         .compareTo( g2.getName() );
            }
        } );

        Group g = result.get( 0 );
        assertThat( g.getName(), equalTo( grp.getName() ) );

        g = result.get( 1 );
        assertThat( g.getName(), equalTo( grp2.getName() ) );
    }

}
