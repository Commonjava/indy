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
package org.commonjava.indy.pkg.npm.content;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.ApplicationStatus;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This case tests if files can be deleted in a remote repo
 * when: <br />
 * <ul>
 *      <li>creates a remote repo and expect file in it</li>
 *      <li>deletes the file in remote repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the file can not be deleted with 400 error</li>
 * </ul>
 */
public class NPMRemoteDeleteFileTest
                extends AbstractContentManagementTest
{
    @Test
    public void test() throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "jquery";
        final String repoName = "test-remote";

        server.expect( server.formatUrl( repoName, path ), 200, stream );

        RemoteRepository remoteRepository = new RemoteRepository( NPM_PKG_KEY, repoName, server.formatUrl( repoName ) );
        remoteRepository = client.stores().create( remoteRepository, "adding npm remote repo", RemoteRepository.class );

        StoreKey storeKey = remoteRepository.getKey();

        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );

        try
        {
            client.content().delete( storeKey, path );
        }
        catch ( IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( ApplicationStatus.BAD_REQUEST.code() ) );
        }

        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );

        stream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
