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
import org.commonjava.aprox.core.live.AbstractAProxLiveTest;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.io.StoreKeySerializer;
import org.commonjava.web.json.model.Listing;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.reflect.TypeToken;

@RunWith( Arquillian.class )
// @Ignore( "TODO - Restore deprecated access to /admin/repository" )
public class DeprecatedRepositoryAdminResourceLiveTest
    extends AbstractAProxLiveTest
{

    private static final String BASE_URL = "/admin/repository";

    private static final String ADJ_BASE_URL = "/admin/repositories";

    @Deployment
    public static WebArchive createWar()
    {
        return createWar( DeprecatedRepositoryAdminResourceLiveTest.class ).build();
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
        final Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        final HttpResponse response = webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CREATED );

        final String repoUrl = webFixture.resourceUrl( BASE_URL, repo.getName() );
        webFixture.assertLocationHeader( response, webFixture.resourceUrl( ADJ_BASE_URL, repo.getName() ) );

        final Repository result = webFixture.get( repoUrl, Repository.class );

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
        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CREATED );

        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CONFLICT );

        final Listing<Repository> result =
            webFixture.getListing( webFixture.resourceUrl( BASE_URL, "/list" ), new TypeToken<Listing<Repository>>()
            {
            } );

        assertThat( result, notNullValue() );

        final List<? extends Repository> items = result.getItems();

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
        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CREATED );

        webFixture.delete( webFixture.resourceUrl( BASE_URL, repo.getName() ) );

        webFixture.get( webFixture.resourceUrl( BASE_URL, repo.getName() ), HttpStatus.SC_NOT_FOUND );
    }

    @Test
    public void createTwoReposAndRetrieveAll()
        throws Exception
    {
        final ArtifactStore repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo, HttpStatus.SC_CREATED );

        final ArtifactStore repo2 = new Repository( "test", "http://www.google.com" );
        webFixture.post( webFixture.resourceUrl( BASE_URL ), repo2, HttpStatus.SC_CREATED );

        final Listing<Repository> result =
            webFixture.getListing( webFixture.resourceUrl( BASE_URL, "/list" ), new TypeToken<Listing<Repository>>()
            {
            } );

        assertThat( result, notNullValue() );

        final List<? extends Repository> repositories = result.getItems();

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
