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

import org.commonjava.indy.client.core.helper.PathInfo;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This case tests if files path-info can be retrieved successfully in a remote repo
 * when: <br />
 * <ul>
 *      <li>creates a remote repo and expect files in it</li>
 *      <li>retrieve the files using corresponding mapping path in the remote repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the files path-info can be retrieved successfully with no error</li>
 * </ul>
 */

public class NPMRemotePathContentTest
                extends AbstractContentManagementTest
{
    @Test
    public void test() throws Exception
    {
        final InputStream stream = new ByteArrayInputStream( ( "This is a test: " + System.nanoTime() ).getBytes() );

        final String packagePath = "jquery";
        final String versionPath = "jquery/1.1.0";
        final String tarballPath = "jquery/-/jquery-1.1.0.tgz";

        server.expect( server.formatUrl( STORE, packagePath ), 200, stream );
        server.expect( server.formatUrl( STORE, versionPath ), 200, stream );
        server.expect( server.formatUrl( STORE, tarballPath ), 200, stream );

        final RemoteRepository remoteRepository = new RemoteRepository( NPM_PKG_KEY, STORE, server.formatUrl( STORE ) );
        final StoreKey storeKey = remoteRepository.getKey();

        client.stores().create( remoteRepository, "adding npm remote repo", RemoteRepository.class );

        final PathInfo result1 = client.content().getInfo( storeKey, packagePath );
        final PathInfo result2 = client.content().getInfo( storeKey, versionPath );
        final PathInfo result3 = client.content().getInfo( storeKey, tarballPath );

        assertThat( "no result", result1, notNullValue() );
        assertThat( "doesn't exist", result1.exists(), equalTo( true ) );

        assertThat( "no result", result2, notNullValue() );
        assertThat( "doesn't exist", result2.exists(), equalTo( true ) );

        assertThat( "no result", result3, notNullValue() );
        assertThat( "doesn't exist", result3.exists(), equalTo( true ) );

        stream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
