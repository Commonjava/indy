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
 * This abstract class will do some preparation work for testing of the metadata content timeout function,
 * like prepare mock remote with a http server, set up test repo, prepare normal content and metadata content.
 *
 */
public abstract class AbstractMetadataTimeoutWorkingTest
        extends AbstractContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer( "repos" );

    protected File pomFile;

    protected File metadataFile;

    protected File archetypeFile;

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    protected final String repoId = "test-repo";

    protected final String pomPath = "org/foo/bar/1.0/bar-1.0.pom";

    protected final String metadataPath = "org/foo/bar/maven-metadata.xml";

    protected final String archetypePath = "archetype-catalog.xml";

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
        client.content().get( remote, repoId, pomPath ).close(); // force storage
        assertThat( "no pom result", pomResult, notNullValue() );
        assertThat( "pom doesn't exist", pomResult.exists(), equalTo( true ) );

        pomFile = Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", MAVEN_PKG_KEY,
                             remote.singularEndpointName() + "-" + repoId, pomPath ).toFile();

        assertThat( "pom doesn't exist: " + pomFile, pomFile.exists(), equalTo( true ) );

        // ensure the metadata exist before the timeout checking
        final PathInfo metadataResult = client.content().getInfo( remote, repoId, metadataPath );
        client.content().get( remote, repoId, metadataPath ).close(); // force storage
        assertThat( "no metadata result", metadataResult, notNullValue() );
        assertThat( "metadata doesn't exist", metadataResult.exists(), equalTo( true ) );

        metadataFile = Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", MAVEN_PKG_KEY,
                                  remote.singularEndpointName() + "-" + repoId, metadataPath ).toFile();

        assertThat( "metadata doesn't exist", metadataFile.exists(), equalTo( true ) );

        // ensure the archetype exist before the timeout checking
        final PathInfo archetypeResult = client.content().getInfo( remote, repoId, archetypePath );
        client.content().get( remote, repoId, archetypePath ).close(); // force storage
        assertThat( "no archetype result", archetypeResult, notNullValue() );
        assertThat( "archetype doesn't exist", archetypeResult.exists(), equalTo( true ) );

        archetypeFile = Paths.get( fixture.getBootOptions().getHomeDir(), "var/lib/indy/storage", MAVEN_PKG_KEY,
                                  remote.singularEndpointName() + "-" + repoId, archetypePath ).toFile();

        assertThat( "archetype doesn't exist: " + archetypeFile, archetypeFile.exists(), equalTo( true ) );

    }

    protected abstract RemoteRepository createRemoteRepository();

}
