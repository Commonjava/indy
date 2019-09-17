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
 * This case tests if files can be stored in a remote repo
 * when: <br />
 * <ul>
 *      <li>creates a remote repo</li>
 *      <li>stores file in the remote repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the file can not be stored with 400 error</li>
 * </ul>
 */
public class NPMRemoteStoreFileTest
                extends AbstractContentManagementTest
{
    @Test
    public void test() throws Exception
    {
        final String content = "This is a test: " + System.nanoTime();
        InputStream stream = new ByteArrayInputStream( content.getBytes() );

        final String path = "jquery";
        final String realPath = "jquery/package.json";
        final String repoName = "test-remote";

        RemoteRepository remoteRepository = new RemoteRepository( NPM_PKG_KEY, repoName, server.formatUrl( repoName ) );
        remoteRepository = client.stores().create( remoteRepository, "adding npm remote repo", RemoteRepository.class );

        StoreKey storeKey = remoteRepository.getKey();

        assertThat( client.content().exists( storeKey, path ), equalTo( false ) );

        try
        {
            client.content().store( storeKey, path, stream );
        }
        catch ( IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( ApplicationStatus.BAD_REQUEST.code() ) );
        }

        // for remote, list path of 'jquery/' will be stored
        assertThat( client.content().exists( storeKey, realPath ), equalTo( false ) );

        stream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
