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
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.nio.file.Paths;
import java.io.InputStream;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test if normal content re-schedule mechanism is working. When content is re-requested during the timeout interval,
 * the timeout for this content should be re-scheduled.
 */
public class ContentRescheduleTimeoutTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Test
    @Category( TimingDependent.class )
    public void timeout()
            throws Exception
    {
        final String repoId = "test-repo";
        final String pomPath = "org/foo/bar/1.0/bar-1.0.pom";
        final String pomUrl = server.formatUrl( repoId, pomPath );
        final int CACHE_TIMEOUT_SECONDS = 6;
        final int CACHE_TIMEOUT_WAITING_MILLISECONDS = 3000;

        // mocking up a http server that expects access to a .pom
        final String datetime = ( new Date() ).toString();
        server.expect( pomUrl, 200, String.format( "pom %s", datetime ) );

        // set up remote repository pointing to the test http server, and timeout little later
        final String changelog = "Timeout Testing: " + name.getMethodName();
        final RemoteRepository repository = new RemoteRepository( repoId, server.formatUrl( repoId ) );
        repository.setCacheTimeoutSeconds( CACHE_TIMEOUT_SECONDS );
        client.stores().create( repository, changelog, RemoteRepository.class );

        // first time trigger normal content storage with timeout, should be 6s
        PathInfo pomResult = client.content().getInfo( remote, repoId, pomPath );
        InputStream is = client.content().get( remote, repoId, pomPath); // force storage
        if ( is != null )
        {
            is.close();
        }
        assertThat( "no pom result", pomResult, notNullValue() );
        assertThat( "pom doesn't exist", pomResult.exists(), equalTo( true ) );

        File pomFile = Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", MAVEN_PKG_KEY,
                                  remote.singularEndpointName() + "-" + repoId, pomPath ).toFile();

        assertThat( "pom doesn't exist", pomFile.exists(), equalTo( true ) );

        // wait for first 3s
        Thread.sleep( CACHE_TIMEOUT_WAITING_MILLISECONDS );

        // as the normal content re-request, the timeout interval should be re-scheduled
        client.content().get( remote, repoId, pomPath );

        // will wait another 3.5s
        Thread.sleep( CACHE_TIMEOUT_WAITING_MILLISECONDS + 500 );
        // as rescheduled in 3s, the new timeout should be 3+6=9s, so the artifact should not be deleted
        assertThat( "artifact should not be removed as it is rescheduled with new request", pomFile.exists(),
                    equalTo( true ) );

        // another round wait for 4.5s
        Thread.sleep( CACHE_TIMEOUT_WAITING_MILLISECONDS + 4500 );
        assertThat( "artifact should be removed when new scheduled cache timeout", pomFile.exists(), equalTo( false ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
