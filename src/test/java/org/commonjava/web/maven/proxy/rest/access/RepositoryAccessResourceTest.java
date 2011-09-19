package org.commonjava.web.maven.proxy.rest.access;

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.commonjava.web.maven.proxy.AbstractAProxLiveTest;
import org.commonjava.web.maven.proxy.data.ProxyDataException;
import org.commonjava.web.maven.proxy.fixture.ProxyConfigProvider;
import org.commonjava.web.maven.proxy.model.Repository;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class RepositoryAccessResourceTest
    extends AbstractAProxLiveTest
{

    private static final String BASE_URL = "http://localhost:8080/test/api/1.0/repository/central/";

    private static File repoRoot;

    @BeforeClass
    public static void setRepoRootDir()
        throws IOException
    {
        repoRoot = File.createTempFile( "repo.root.", ".dir" );
        System.setProperty( ProxyConfigProvider.REPO_ROOT_DIR, repoRoot.getAbsolutePath() );
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
        proxyManager.storeRepository( new Repository( "central",
                                                     "http://repo1.maven.apache.org/maven2/" ) );
    }

    @Test
    public void retrievePOMFromProxiedRepository()
        throws ClientProtocolException, IOException
    {
        String response =
            getString( BASE_URL + "org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom",
                       HttpStatus.SC_OK );
        assertThat( response.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

}
