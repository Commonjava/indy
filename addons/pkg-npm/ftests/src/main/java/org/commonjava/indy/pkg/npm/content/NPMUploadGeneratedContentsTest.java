/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class NPMUploadGeneratedContentsTest
                extends AbstractContentManagementTest
{
    @Test
    public void test() throws Exception
    {
        final InputStream CONTENT =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-with-tarball.json" );

        final String path = "jquery";
        final String tarballPath = "jquery/-/jquery-1.5.1.tgz";
        final String versionPath = "jquery/1.5.1";

        final String repoName = "test-hosted";
        HostedRepository repo = new HostedRepository( NPM_PKG_KEY, repoName );

        repo = client.stores().create( repo, "adding npm hosted repo", HostedRepository.class );

        StoreKey storeKey = repo.getKey();
        assertThat( client.content().exists( storeKey, path ), equalTo( false ) );

        client.content().store( storeKey, path, CONTENT );

        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );
        assertThat( client.content().exists( storeKey, tarballPath ), equalTo( true ) );
        assertThat( client.content().exists( storeKey, versionPath ), equalTo( true ) );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
