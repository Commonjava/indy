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
import org.junit.Rule;

import java.io.File;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public abstract class AbstractMetadataTimeoutWorkingTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    protected final String repoId = "test-repo";

    protected final String pomPath = "org/foo/bar/1.0/bar-1.0.pom";

    protected final String metadataPath = "org/foo/bar/maven-metadata.xml";

    protected final String archetypePath = "archetype-catalog.xml";

    protected String pomFilePath;

    protected String metadataFilePath;

    protected String archetypeFilePath;

    @Before
    public void setupRepo()
            throws Exception
    {

        final String pomUrl = server.formatUrl( repoId, pomPath );
        final String metadataUrl = server.formatUrl( repoId, metadataPath );
        final String archetypeUrl = server.formatUrl( repoId, archetypePath );

        // mocking up a http server that expects access to a .pom
        final String datetime = ( new Date() ).toString();
        server.expect( pomUrl, 200, String.format( "pom %s", datetime ) );
        server.expect( metadataUrl, 200, String.format( "metadata %s", datetime ) );
        server.expect( archetypeUrl, 200, String.format( "archetype %s", datetime ) );

        // set up remote repository pointing to the test http server, and timeout little later
        final String changelog = "Timeout Testing: " + name.getMethodName();
        final RemoteRepository repository = createRemoteRepository();

        client.stores().create( repository, changelog, RemoteRepository.class );

        // ensure the pom exist before the timeout checking
        final PathInfo pomResult = client.content().getInfo( remote, repoId, pomPath );
        assertThat( "no pom result", pomResult, notNullValue() );
        assertThat( "pom doesn't exist", pomResult.exists(), equalTo( true ) );
        pomFilePath = String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getIndyHome(),
                                     remote.name(), repoId, pomPath );
        final File pomFile = new File( pomFilePath );
        assertThat( "pom doesn't exist", pomFile.exists(), equalTo( true ) );

        // ensure the metadata exist before the timeout checking
        final PathInfo metadataResult = client.content().getInfo( remote, repoId, metadataPath );
        assertThat( "no metadata result", metadataResult, notNullValue() );
        assertThat( "metadata doesn't exist", metadataResult.exists(), equalTo( true ) );
        metadataFilePath = String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getIndyHome(),
                                          remote.name(), repoId, metadataPath );
        final File metadataFile = new File( metadataFilePath );
        assertThat( "metadata doesn't exist", metadataFile.exists(), equalTo( true ) );

        // ensure the archetype exist before the timeout checking
        final PathInfo archetypeResult = client.content().getInfo( remote, repoId, archetypePath );
        assertThat( "no archetype result", archetypeResult, notNullValue() );
        assertThat( "archetype doesn't exist", archetypeResult.exists(), equalTo( true ) );
        archetypeFilePath = String.format( "%s/var/lib/indy/storage/%s-%s/%s", fixture.getBootOptions().getIndyHome(),
                                          remote.name(), repoId, archetypePath );
        final File archetypeFile = new File( archetypeFilePath );
        assertThat( "archetype doesn't exist", archetypeFile.exists(), equalTo( true ) );

    }

    protected abstract RemoteRepository createRemoteRepository();

}
