/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.PathStyle;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_GENERIC_HTTP;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Root '/' is handled as a special file in generic-http repo. Some build downloads from root of remote site
 * and store the file by hashing the '/' to a special path.
 *
 * This test stores the '/' file and retrieves it. See also {@link org.commonjava.indy.content.IndyPathGenerator}
 * and {@link org.commonjava.indy.core.bind.jaxrs.GenericContentAccessResource}
 */
public class StoreFileAndVerifyRootFileInGenericRepoTest
                extends AbstractContentManagementTest
{
    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    private final String expected = "This is a test: " + System.nanoTime();

    private final String rootPath = "/";

    @Test
    public void getRootFileAndVerifyOnRemote() throws Exception
    {
        // CASE 1: store and retrieve on remote repo by the "root" path.
        
        final String remoteUrl = server.formatUrl( rootPath );
        server.expect( remoteUrl, 200, expected );

        // Create remote repo
        RemoteRepository remote1 = new RemoteRepository( PKG_TYPE_GENERIC_HTTP, "repo1", remoteUrl );
        remote1.setPathStyle( PathStyle.hashed );
        remote1 = client.stores()
                        .create( remote1, "add generic-http remote repo with hashed path-style",
                                 RemoteRepository.class );

        // Get and verify
        assertThat( client.content().exists( remote1.getKey(), rootPath ), equalTo( true ) );
        assertContent( remote1, rootPath, expected );
    }

    @Test
    public void storeRootFileAndVerifyOnHosted() throws Exception
    {
        // CASE 2: store and retrieve on hosted repo by the "root" path.

        HostedRepository hosted1 = new HostedRepository( PKG_TYPE_GENERIC_HTTP, STORE );
        hosted1.setPathStyle( PathStyle.hashed );
        hosted1 = this.client.stores()
                             .create( hosted1, "add generic-http hosted repo with hashed path-style",
                                      HostedRepository.class );

        // Store and verify
        client.content().store( hosted1.getKey(), rootPath, new ByteArrayInputStream( expected.getBytes() ) );

        assertThat( client.content().exists( hosted1.getKey(), rootPath ), equalTo( true ) );
        assertContent( hosted1, rootPath, expected );
    }
}
