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
package org.commonjava.aprox.core.live.rest.admin;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.commonjava.aprox.core.fixture.ProxyConfigProvider;
import org.commonjava.aprox.core.live.AbstractAProxLiveTest;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.io.StoreKeySerializer;
import org.commonjava.web.common.model.Listing;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
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

    private static final String BASE_URL = "http://localhost:8080/test/api/1.0/admin/repository";

    @Deployment
    public static WebArchive createWar()
    {
        return new TestWarArchiveBuilder( RepositoryAdminResourceLiveTest.class ).withExtraClasses( AbstractAProxLiveTest.class,
                                                                                                    ProxyConfigProvider.class )
                                                                                 .build();
    }

    @Before
    public void registerSerializer()
    {
        serializer.registerSerializationAdapters( new StoreKeySerializer() );
    }

    @Test
    public void createAndRetrieveCentralRepoProxy()
        throws Exception
    {
        final Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        final HttpResponse response = post( BASE_URL, repo, HttpStatus.SC_CREATED );

        final String repoUrl = BASE_URL + "/" + repo.getName();
        assertLocationHeader( response, repoUrl );

        final Repository result = get( repoUrl, Repository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.getUrl(), equalTo( repo.getUrl() ) );
        assertThat( result.getUser(), nullValue() );
        assertThat( result.getPassword(), nullValue() );
    }

    @Test
    public void createCentralRepoProxyTwiceAndRetrieveOne()
        throws Exception
    {
        final Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        post( BASE_URL, repo, HttpStatus.SC_CREATED );

        post( BASE_URL, repo, HttpStatus.SC_CONFLICT );

        final Listing<Repository> result = getListing( BASE_URL + "/list", new TypeToken<Listing<Repository>>()
        {
        } );

        assertThat( result, notNullValue() );

        final List<Repository> items = result.getItems();

        assertThat( items, notNullValue() );
        assertThat( items.size(), equalTo( 1 ) );

        final Repository r = items.get( 0 );
        assertThat( r.getName(), equalTo( repo.getName() ) );
        assertThat( r.getUrl(), equalTo( repo.getUrl() ) );
    }

    @Test
    public void createAndDeleteCentralRepoProxy_ByName()
        throws Exception
    {
        final ArtifactStore repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        post( BASE_URL, repo, HttpStatus.SC_CREATED );

        delete( BASE_URL + "/" + repo.getName() );

        get( BASE_URL + "/" + repo.getName(), HttpStatus.SC_NOT_FOUND );
    }

    @Test
    public void createTwoReposAndRetrieveAll()
        throws Exception
    {
        final ArtifactStore repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        post( BASE_URL, repo, HttpStatus.SC_CREATED );

        final ArtifactStore repo2 = new Repository( "test", "http://www.google.com" );
        post( BASE_URL, repo2, HttpStatus.SC_CREATED );

        final Listing<Repository> result = getListing( BASE_URL + "/list", new TypeToken<Listing<Repository>>()
        {
        } );

        assertThat( result, notNullValue() );

        final List<Repository> repositories = result.getItems();

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
