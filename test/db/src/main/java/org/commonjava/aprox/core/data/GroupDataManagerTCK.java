/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

import org.apache.log4j.Level;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.util.logging.Log4jUtil;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class GroupDataManagerTCK
    extends AbstractProxyDataManagerTCK
{

    @BeforeClass
    public static void setupLogging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

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
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        manager.storeRepository( new Repository( "central", "http://repo1.maven.apache.org/maven2/" ) );
        manager.storeRepository( new Repository( "repo2", "http://repo1.maven.org/maven2/" ) );
    }

    @Test
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void createAndRetrieveEmptyGroup()
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp = new Group( "test" );

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
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp = new Group( "test" );

        manager.storeGroup( grp );

        manager.deleteGroup( grp.getName() );

        final Group result = manager.getGroup( grp.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndDeleteGroup_ByObject()
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp = new Group( "test" );

        manager.storeGroup( grp );

        manager.deleteGroup( grp );

        final Group result = manager.getGroup( grp.getName() );

        assertThat( result, nullValue() );
    }

    @Test
    public void createAndRetrieveGroupWithTwoConstituents()
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp =
            new Group( "test", new StoreKey( StoreType.repository, "central" ), new StoreKey( StoreType.repository,
                                                                                              "repo2" ) );

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
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp =
            new Group( "test", new StoreKey( StoreType.repository, "repo2" ), new StoreKey( StoreType.repository,
                                                                                            "central" ) );

        manager.storeGroup( grp );

        final List<? extends ArtifactStore> repos = manager.getOrderedConcreteStoresInGroup( grp.getName() );

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
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp =
            new Group( "test", new StoreKey( StoreType.repository, "central" ), new StoreKey( StoreType.repository,
                                                                                              "repo2" ) );

        manager.storeGroup( grp );

        final List<? extends ArtifactStore> result = manager.getOrderedConcreteStoresInGroup( grp.getName() );

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
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp = new Group( "test" );

        manager.storeGroup( grp, true );
        manager.storeGroup( grp, true );

        final List<? extends Group> result = manager.getAllGroups();

        assertThat( result, notNullValue() );
        assertThat( result.size(), equalTo( 1 ) );
    }

    @Test
    public void createTwoGroupsAndRetrieveBoth()
        throws ProxyDataException
    {
        final StoreDataManager manager = getFixtureProvider().getDataManager();

        final Group grp = new Group( "test" );
        final Group grp2 = new Group( "test2" );

        manager.storeGroup( grp );
        manager.storeGroup( grp2 );

        final List<? extends Group> result = manager.getAllGroups();

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
