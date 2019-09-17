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

import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This case tests if files can be generated for two versions uploads in a hosted repo
 * when: <br />
 * <ul>
 *      <li>creates a hosted repo</li>
 *      <li>uploads two versions of metas to the hosted repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the tarball and version meta can be generated successfully with no error</li>
 * </ul>
 */
public class NPMTwoTimesUploadsGeneratedContentsTest
        extends AbstractContentManagementTest
{
    @Test
    public void test()
            throws Exception
    {
        final InputStream content1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.5.1.json" );

        final InputStream content2 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.6.2.json" );

        final String path = "jquery";

        final String firstTarballPath = "jquery/-/jquery-1.5.1.tgz";
        final String firstVersionPath = "jquery/1.5.1";

        final String secondTarballPath = "jquery/-/jquery-1.6.2.tgz";
        final String secondVersionPath = "jquery/1.6.2";

        final String repoName = "test-hosted";
        HostedRepository repo = new HostedRepository( NPM_PKG_KEY, repoName );

        repo = client.stores().create( repo, "adding npm hosted repo", HostedRepository.class );

        StoreKey storeKey = repo.getKey();

        client.content().store( storeKey, path, content1 );
        client.content().store( storeKey, path, content2 );

        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );

        assertThat( client.content().exists( storeKey, firstTarballPath ), equalTo( true ) );
        assertThat( client.content().exists( storeKey, firstVersionPath ), equalTo( true ) );

        assertThat( client.content().exists( storeKey, secondTarballPath ), equalTo( true ) );
        assertThat( client.content().exists( storeKey, secondVersionPath ), equalTo( true ) );

        content1.close();
        content2.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
