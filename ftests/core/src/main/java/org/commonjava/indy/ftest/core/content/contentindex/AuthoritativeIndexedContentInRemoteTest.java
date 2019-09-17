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
package org.commonjava.indy.ftest.core.content.contentindex;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>Authoritative index of content index enabled</li>
 *     <li>A remote repo</li>
 *     <li>Remote path one, will be available before repo set to authoritative index enabled first time</li>
 *     <li>Remote path two, will be available after repo set to authoritative index enabled first time</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Fetch first path once but not second one</li>
 *     <li>Enable authoritative index of the remote repo</li>
 *     <li>Fetch first and second path</li>
 *     <li>Disable authoritative index of the remote repo</li>
 *     <li>Enable authoritative index of the remote repo again</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>When repo is authoritative index enabled first time, the first path can be fetched, but the second path can not</li>
 *     <li>When repo is authoritative index disabled, both path are available, and after fetching they are both indexed</li>
 *     <li>When repo is authoritative index enabled again, both path are still available</li>
 * </ul>
 */
public class AuthoritativeIndexedContentInRemoteTest
        extends AbstractIndyFunctionalTest
{

    private static final String FIRST_PATH = "org/foo/bar/1/first.txt";

    private static final String SECOND_PATH = "org/foo/bar/2/second.txt";

    private static final String FIRST_PATH_CONTENT = "first content";

    private static final String SECOND_PATH_CONTENT = "second content";

    @Rule
    public ExpectationServer server = new ExpectationServer();

    @Test
    public void bypassNotIndexedContentWithAuthoritativeIndex()
            throws Exception
    {

        final String repoName = newName();
        RemoteRepository repo = new RemoteRepository( MAVEN_PKG_KEY, repoName, server.formatUrl( repoName ) );
        repo = client.stores().create( repo, name.getMethodName(), RemoteRepository.class );

        server.expect( server.formatUrl( repoName, FIRST_PATH ), 200, FIRST_PATH_CONTENT );

        try (InputStream first = client.content().get( repo.getKey(), FIRST_PATH ))
        {
            assertThat( IOUtils.toString( first ), equalTo( FIRST_PATH_CONTENT ) );
        }

        repo.setAuthoritativeIndex( true );
        assertThat( client.stores().update( repo, name.getMethodName() ), equalTo( true ) );

        server.expect( server.formatUrl( repoName, SECOND_PATH ), 200, SECOND_PATH_CONTENT );

        try (InputStream first = client.content().get( repo.getKey(), FIRST_PATH ))
        {
            assertThat( IOUtils.toString( first ), equalTo( FIRST_PATH_CONTENT ) );
        }

        try (InputStream second = client.content().get( repo.getKey(), SECOND_PATH ))
        {
            assertThat( second, equalTo( null ) );
        }

        repo.setAuthoritativeIndex( false );
        assertThat( client.stores().update( repo, name.getMethodName() ), equalTo( true ) );


        try (InputStream first = client.content().get( repo.getKey(), FIRST_PATH ))
        {
            assertThat( IOUtils.toString( first ), equalTo( FIRST_PATH_CONTENT ) );
        }

        try (InputStream second = client.content().get( repo.getKey(), SECOND_PATH ))
        {
            assertThat( IOUtils.toString( second ), equalTo( SECOND_PATH_CONTENT ) );
        }

        repo.setAuthoritativeIndex( true );
        assertThat( client.stores().update( repo, name.getMethodName() ), equalTo( true ) );

        try (InputStream first = client.content().get( repo.getKey(), FIRST_PATH ))
        {
            assertThat( IOUtils.toString( first ), equalTo( FIRST_PATH_CONTENT ) );
        }

        try (InputStream second = client.content().get( repo.getKey(), SECOND_PATH ))
        {
            assertThat( IOUtils.toString( second ), equalTo( SECOND_PATH_CONTENT ) );
        }

    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/content-index.conf", "[content-index]\nsupport.authoritative.indexes=true" );
    }
}
