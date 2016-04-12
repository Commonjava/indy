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
package org.commonjava.indy.ftest.core.content;

import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ContentTimeoutWorkingTest
        extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    private String repoId;

    private String pomPath;

    private String pomFilePath;

    public static final int TIMEOUT_SECONDS = 2;

    public static final int TIMEOUT_WAITING_MILLISECONDS = ( TIMEOUT_SECONDS + 2 ) * 1000;

    @Before
    public void setupRepo()
            throws Exception
    {
        repoId = "test-repo";
        pomPath = "org/foo/bar/1.0/bar-1.0.pom";
        final String pomUrl = server.formatUrl( repoId, pomPath );

        // mocking up a http server that expects access to a .pom
        final String datetime = ( new Date() ).toString();
        server.expect( pomUrl, 200, String.format( "pom %s", datetime ) );

        // set up remote repository pointing to the test http server, and timeout little later
        final String changelog = "Timeout Testing: " + name.getMethodName();
        final RemoteRepository repository = new RemoteRepository( repoId, server.formatUrl( repoId ) );
        repository.setTimeoutSeconds( TIMEOUT_SECONDS );

        client.stores().create( repository, changelog, RemoteRepository.class );

        // ensure the pom exist before the timeout checking
        final PathInfo result = client.content().getInfo( remote, repoId, pomPath );
        assertThat( "no result", result, notNullValue() );
        assertThat( "doesn't exist", result.exists(), equalTo( true ) );
        pomFilePath = String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getIndyHome(),
                                     remote.name(), repoId, pomPath );
        final File pomFile = new File( pomFilePath );
        assertThat( "pom doesn't exist", pomFile.exists(), equalTo( true ) );

    }

    @Ignore
    @Test
    public void quartzBasedTimeoutArtifact()
            throws Exception
    {
        // make sure the repo timout
        Thread.sleep( TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", TIMEOUT_SECONDS );

        final File pomFile = new File( pomFilePath );
        assertThat( "artifact should be removed when timeout", pomFile.exists(), equalTo( false ) );
    }

    @Ignore
    @Test
    public void cacheProviderBasedTimeoutArtifact()
            throws Exception
    {
        final File pomFile = new File( pomFilePath );
        final long pomLastModified = pomFile.lastModified();

        // make sure the repo timout
        Thread.sleep( TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", TIMEOUT_SECONDS );

        final Boolean contentExists = client.content().exists( remote, repoId, pomPath );
        final File pomFileAgain = new File( pomFilePath );
        if ( contentExists && pomFileAgain.exists() )
        {
            assertThat( "cache timout not working, artifact not removed and not changed", pomFileAgain.lastModified(),
                        is( not( pomLastModified ) ) );
        }
    }

}
