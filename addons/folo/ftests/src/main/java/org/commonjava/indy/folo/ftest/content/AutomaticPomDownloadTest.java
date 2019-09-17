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
package org.commonjava.indy.folo.ftest.content;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test checking that a pom is downloaded automatically without requesting it. The test runs an http server that expects
 * download of /org/foo/bar/1.0/bar-1.0.jar and /org/foo/bar/1.0/bar-1.0.pom. Then the test ensures that the pom does
 * not exist, downloads the jar and after a timeout it checks that the pom exists.
 *
 * @author pkocandr
 */
public class AutomaticPomDownloadTest
    extends AbstractFoloContentManagementTest
{

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void downloadJarAndCheckIfPomWasDownloaded()
        throws Exception
    {
        final String repoId = "test-repo";
        final String pathFormat = "/org/foo/bar/1.0/bar-1.0.%s";
        final String jarPath = String.format( pathFormat, "jar" );
        final String jarUrl = server.formatUrl( repoId, jarPath );
        final String pomPath = String.format( pathFormat, "pom" );
        final String pomUrl = server.formatUrl( repoId, pomPath );

        // mocking up a http server that expects access to both a .jar and the accompanying .pom
        String datetime = ( new Date() ).toString();
        server.expect( pomUrl, 200, String.format( "pom %s", datetime ) );
        server.expect( jarUrl, 200, String.format( "jar %s", datetime ) );

        // set up remote repository pointing to the test http server
        final String changelog = "Setup: " + name.getMethodName();
        client.stores().create( new RemoteRepository( repoId, server.formatUrl( repoId ) ), changelog,
                                RemoteRepository.class );

        // ensure the pom does not exist before the jar download
        assertThat( client.content().exists( remote, repoId, pomPath, true ), equalTo( false ) );

        // download the .jar
        IndyFoloContentClientModule clientModule = client.module( IndyFoloContentClientModule.class );
        final InputStream result = clientModule.get( newName(), remote, repoId, jarPath );
        assertThat( result, notNullValue() );

        result.close();

        // verify the existence of the .pom file after sleeping a bit to allow the event to propagate
        Thread.sleep( 3000 );

        assertThat( client.content().exists( remote, repoId, pomPath, true ), equalTo( true ) );
    }

}
