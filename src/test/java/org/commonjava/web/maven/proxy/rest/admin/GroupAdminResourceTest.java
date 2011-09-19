package org.commonjava.web.maven.proxy.rest.admin;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.commonjava.web.common.model.Listing;
import org.commonjava.web.maven.proxy.AbstractAProxLiveTest;
import org.commonjava.web.maven.proxy.model.Group;
import org.commonjava.web.maven.proxy.model.Repository;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.reflect.TypeToken;

@RunWith( Arquillian.class )
public class GroupAdminResourceTest
    extends AbstractAProxLiveTest
{

    private static final String BASE_URL = "http://localhost:8080/test/api/1.0/admin/group";

    @Before
    public void seedRepositoriesForGroupTests()
        throws Exception
    {
        proxyManager.storeRepository( new Repository( "central",
                                                     "http://repo1.maven.apache.org/maven2/" ) );
        proxyManager.storeRepository( new Repository( "repo2", "http://repo1.maven.org/maven2/" ) );
    }

    @Test
    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public void createAndRetrieveEmptyGroup()
        throws Exception
    {
        Group grp = new Group( "test" );

        HttpResponse response = post( BASE_URL, grp, HttpStatus.SC_CREATED );
        assertLocationHeader( response, BASE_URL + "/" + grp.getName() );

        Group result = get( BASE_URL + "/" + grp.getName(), Group.class );

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
    public void createAndDeleteGroup()
        throws Exception
    {
        Group grp = new Group( "test" );

        post( BASE_URL, grp, HttpStatus.SC_CREATED );

        delete( BASE_URL + "/" + grp.getName() );

        get( BASE_URL + "/" + grp.getName(), HttpStatus.SC_NOT_FOUND );
    }

    @Test
    public void createAndRetrieveGroupWithTwoConstituents()
        throws Exception
    {
        Group grp = new Group( "test", "central", "repo2" );

        post( BASE_URL, grp, HttpStatus.SC_CREATED );

        Group result = get( BASE_URL + "/" + grp.getName(), Group.class );

        assertThat( result, notNullValue() );
        assertThat( result.getName(), equalTo( grp.getName() ) );

        List<String> repos = result.getConstituents();
        assertThat( repos, notNullValue() );
        assertThat( repos.size(), equalTo( 2 ) );

        assertThat( repos.get( 0 ), equalTo( "central" ) );
        assertThat( repos.get( 1 ), equalTo( "repo2" ) );
    }

    @Test
    public void createSameGroupTwiceAndRetrieveOne()
        throws Exception
    {
        Group grp = new Group( "test" );

        post( BASE_URL, grp, HttpStatus.SC_CREATED );
        post( BASE_URL, grp, HttpStatus.SC_CONFLICT );

        Listing<Group> result = getListing( BASE_URL + "/list", new TypeToken<Listing<Group>>()
        {} );

        assertThat( result, notNullValue() );

        List<Group> items = result.getItems();
        assertThat( items.size(), equalTo( 1 ) );
    }

    @Test
    public void createTwoGroupsAndRetrieveBoth()
        throws Exception
    {
        Group grp = new Group( "test" );
        Group grp2 = new Group( "test2" );

        post( BASE_URL, grp, HttpStatus.SC_CREATED );
        post( BASE_URL, grp2, HttpStatus.SC_CREATED );

        Listing<Group> result = getListing( BASE_URL + "/list", new TypeToken<Listing<Group>>()
        {} );

        assertThat( result, notNullValue() );

        List<Group> items = result.getItems();
        assertThat( items.size(), equalTo( 2 ) );

        Collections.sort( items, new Comparator<Group>()
        {
            @Override
            public int compare( final Group g1, final Group g2 )
            {
                return g1.getName().compareTo( g2.getName() );
            }
        } );

        Group g = items.get( 0 );
        assertThat( g.getName(), equalTo( grp.getName() ) );

        g = items.get( 1 );
        assertThat( g.getName(), equalTo( grp2.getName() ) );
    }

}
