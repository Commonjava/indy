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
package org.commonjava.aprox.live.rest.admin;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.commonjava.aprox.live.AbstractAProxLiveTest;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.web.json.model.Listing;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.reflect.TypeToken;

@RunWith( Arquillian.class )
public class RepositoryAdminResourceLiveTest
    extends AbstractAProxLiveTest
{

    private static final String BASE_URL = "/admin/repositories";

    @Deployment
    public static WebArchive createWar()
    {
        return createWar( RepositoryAdminResourceLiveTest.class ).build();
    }

    @Before
    public void registerSerializer()
    {
        webFixture.getSerializer()
                  .registerSerializationAdapters( new StoreKeySerializer() );
    }

    @Test
    public void createAndRetrieveCentralRepoProxy()
        throws Exception
    {
        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        final HttpResponse response = webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CREATED );

        final String repoUrl = webFixture.resourceUrl( BASE_URL, repo.getName() );
        webFixture.assertLocationHeader( response, repoUrl );

        final RemoteRepository result = webFixture.get( repoUrl, RemoteRepository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.getUrl(), equalTo( repo.getUrl() ) );
        assertThat( result.getUser(), nullValue() );
        assertThat( result.getPassword(), nullValue() );
    }

    @Test
    public void createCentralRepoProxyTwiceAndRetrieveOne()
        throws Exception
    {
        final RemoteRepository repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CREATED );

        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CONFLICT );

        final Listing<RemoteRepository> result =
            webFixture.getListing( webFixture.resourceUrl( BASE_URL ), new TypeToken<Listing<RemoteRepository>>()
            {
            } );

        assertThat( result, notNullValue() );

        final List<? extends RemoteRepository> items = result.getItems();

        assertThat( items, notNullValue() );
        assertThat( items.size(), equalTo( 1 ) );

        final RemoteRepository r = items.get( 0 );
        assertThat( r.getName(), equalTo( repo.getName() ) );
        assertThat( r.getUrl(), equalTo( repo.getUrl() ) );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByName()
        throws Exception
    {
        final ArtifactStore repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CREATED );

        webFixture.delete( webFixture.resourceUrl( BASE_URL, repo.getName() ) );

        webFixture.get( webFixture.resourceUrl( BASE_URL, repo.getName() ), HttpStatus.SC_NOT_FOUND );
    }

    @Test
    public void createTwoReposAndRetrieveAll()
        throws Exception
    {
        final ArtifactStore repo = new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" );
        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CREATED );

        final ArtifactStore repo2 = new RemoteRepository( "test", "http://www.google.com" );
        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo2, HttpStatus.SC_CREATED );

        final Listing<RemoteRepository> result =
            webFixture.getListing( webFixture.resourceUrl( BASE_URL ), new TypeToken<Listing<RemoteRepository>>()
            {
            } );

        assertThat( result, notNullValue() );

        final List<? extends RemoteRepository> repositories = result.getItems();

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

}
