/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;

import java.io.File;
import java.nio.file.Paths;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This abstract class will do some preparation work for testing of the content timeout function,
 * like prepare mock remote with a http server, set up test repo.
 *
 */
public abstract class AbstractContentTimeoutWorkingTest
        extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    private File pomFile;

    protected static final int TIMEOUT_SECONDS = 1;

    protected static final int TIMEOUT_WAITING_MILLISECONDS = 2500;

    @Before
    public void setupRepo()
            throws Exception
    {
        final String repoId = "test-repo";
        final String pomPath = "org/foo/bar/1.0/bar-1.0.pom";
        final String pomUrl = server.formatUrl( repoId, pomPath );

        // mocking up a http server that expects access to a .pom
        final String datetime = ( new Date() ).toString();
        server.expect( pomUrl, 200, String.format( "pom %s", datetime ) );

        // set up remote repository pointing to the test http server, and timeout little later
        final String changelog = "Timeout Testing: " + name.getMethodName();
        final RemoteRepository repository = createRemoteRepository( repoId );

        client.stores().create( repository, changelog, RemoteRepository.class );

        // ensure the pom exist before the timeout checking
        final PathInfo result = client.content().getInfo( remote, repoId, pomPath );
        assertThat( "no result", result, notNullValue() );
        assertThat( "doesn't exist", result.exists(), equalTo( true ) );

        client.content().get(remote, repoId, pomPath).close(); // force storage

        pomFile =
                Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", MAVEN_PKG_KEY, remote.singularEndpointName() + "-" + repoId,
                           pomPath ).toFile();

        assertThat( "pom doesn't exist: " + pomFile, this.pomFile.exists(), equalTo( true ) );

    }

    protected void fileCheckingAfterTimeout()
            throws Exception
    {
        // make sure the repo timout
        Thread.sleep( getTestTimeoutMultiplier() * TIMEOUT_WAITING_MILLISECONDS );
        logger.debug( "Timeout time {}s passed!", TIMEOUT_SECONDS );

        assertThat( "artifact should be removed when timeout", pomFile.exists(), equalTo( false ) );
    }

    protected abstract RemoteRepository createRemoteRepository( String repoId );
}
