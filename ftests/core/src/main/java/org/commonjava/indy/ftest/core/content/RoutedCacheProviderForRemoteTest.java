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
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;

import static org.commonjava.indy.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RoutedCacheProviderForRemoteTest
        extends AbstractContentManagementTest
{
    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void proxyRemoteAndNFSNotSetup()
            throws Exception
    {
        final String repo1 = "repo1";
        final String pomPath = "org/foo/bar/1.0/bar-1.0.pom";
        final String pomUrl = server.formatUrl( repo1, pomPath );

        final String datetime = ( new Date() ).toString();
        server.expect( pomUrl, 200, String.format( "pom %s", datetime ) );

        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );

        client.stores().create( remote1, "adding remote", RemoteRepository.class );
        final PathInfo result = client.content().getInfo( remote, repo1, pomPath );

        assertThat( "no result", result, notNullValue() );
        assertThat( "doesn't exist", result.exists(), equalTo( true ) );

        final File nfsStorage = Paths.get( fixture.getBootOptions().getIndyHome(), NFS_BASE ).toFile();
        assertThat( nfsStorage.exists(), equalTo( true ) );
        assertThat( nfsStorage.list().length, equalTo( 0 ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        writeConfigFile( "main.conf", readTestResource( "default-test-main.conf" ) );
    }

}
