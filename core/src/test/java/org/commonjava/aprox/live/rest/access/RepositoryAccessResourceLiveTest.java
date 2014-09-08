/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
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
        proxyManager.storeRemoteRepository( new RemoteRepository( "central", "http://repo1.maven.apache.org/maven2/" ),
                                            "test setup" );
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
