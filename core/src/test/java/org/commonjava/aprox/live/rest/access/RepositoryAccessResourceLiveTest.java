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
package org.commonjava.aprox.live.rest.access;

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.live.AbstractAProxLiveTest;
import org.commonjava.aprox.live.fixture.ProxyConfigProvider;
import org.commonjava.aprox.model.RemoteRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class RepositoryAccessResourceLiveTest
    extends AbstractAProxLiveTest
{

    private static final String BASE_URL = "/repository/central/";

    private static File repoRoot;

    @BeforeClass
    public static void setRepoRootDir()
        throws IOException
    {
        repoRoot = File.createTempFile( "repo.root.", ".dir" );
        System.setProperty( ProxyConfigProvider.REPO_ROOT_DIR, repoRoot.getAbsolutePath() );
    }

    @Deployment
    public static WebArchive createWar()
    {
        return createWar( RepositoryAccessResourceLiveTest.class ).build();
    }

    @AfterClass
    public static void clearRepoRootDir()
        throws IOException
    {
        if ( repoRoot != null && repoRoot.exists() )
        {
            forceDelete( repoRoot );
        }
    }

    @Before
    public void setupTest()
        throws ProxyDataException
    {
        proxyManager.storeRemoteRepository( new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" ) );
    }

    @Test
    public void retrievePOMFromProxiedRepository()
        throws ClientProtocolException, IOException
    {
        final String response =
            webFixture.getString( webFixture.resourceUrl( BASE_URL,
                                                          "org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom" ),
                                  HttpStatus.SC_OK );
        assertThat( response.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

}
