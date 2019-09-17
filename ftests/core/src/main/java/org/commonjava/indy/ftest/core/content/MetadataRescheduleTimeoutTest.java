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
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.nio.file.Paths;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test if metadata content re-schedule mechanism is working. When metadata is re-requested during the timeout interval,
 * the timeout for this content should NOT be re-scheduled.
 */
public class MetadataRescheduleTimeoutTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Test
    @Category( TimingDependent.class )
    public void timeout()
            throws Exception
    {
        final int METADATA_TIMEOUT_SECONDS = 4;
        final int METADATA_TIMEOUT_WAITING_MILLISECONDS = 2500;

        final String repoId = "test-repo";
        final String metadataPath = "org/foo/bar/maven-metadata.xml";

        final String metadataUrl = server.formatUrl( repoId, metadataPath );

        // mocking up a http server that expects access to metadata
        final String datetime = ( new Date() ).toString();
        server.expect( metadataUrl, 200, String.format( "metadata %s", datetime ) );

        // set up remote repository pointing to the test http server, and timeout little later
        final String changelog = "Timeout Testing: " + name.getMethodName();
        final RemoteRepository repository = new RemoteRepository( repoId, server.formatUrl( repoId ) );
        repository.setMetadataTimeoutSeconds( METADATA_TIMEOUT_SECONDS );
        client.stores().create( repository, changelog, RemoteRepository.class );

        // first time trigger normal content storage with timeout, should be 4s
        PathInfo pomResult = client.content().getInfo( remote, repoId, metadataPath );
        client.content().get( remote, repoId, metadataPath ).close(); // force storage

        assertThat( "no metadata result", pomResult, notNullValue() );
        assertThat( "metadata doesn't exist", pomResult.exists(), equalTo( true ) );

        File metadataFile = Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", MAVEN_PKG_KEY,
                                       remote.singularEndpointName() + "-" + repoId, metadataPath ).toFile();

        assertThat( "metadata doesn't exist", metadataFile.exists(), equalTo( true ) );

        // wait for first 2.5s
        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS );

        // as the metadata content re-request, the metadata timeout interval should NOT be re-scheduled
        client.content().get( remote, repoId, metadataPath ).close();

        // will wait another 4s
        Thread.sleep( METADATA_TIMEOUT_WAITING_MILLISECONDS + 1500 );
        // as rescheduled, the artifact should not be deleted
        assertThat( "artifact should be removed as the rescheduled of metadata should not succeed",
                    metadataFile.exists(), equalTo( false ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
