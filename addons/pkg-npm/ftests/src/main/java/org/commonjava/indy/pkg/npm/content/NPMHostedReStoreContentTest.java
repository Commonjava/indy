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
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.commonjava.indy.pkg.npm.model.VersionMetadata;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This case tests if files can be re-stored in a hosted repo
 * when: <br />
 * <ul>
 *      <li>creates a hosted repo</li>
 *      <li>stores file in hosted repo once</li>
 *      <li>updates the files content in hosted repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the file can be updated successfully with no error</li>
 * </ul>
 */
public class NPMHostedReStoreContentTest
                extends AbstractContentManagementTest
{
    @Test
    public void test() throws Exception
    {
        final InputStream content1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.5.1.json" );
        final InputStream content2 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.6.2.json" );

        final String path = "jquery";

        final String repoName = "test-hosted";
        HostedRepository repo = new HostedRepository( NPM_PKG_KEY, repoName );

        repo = client.stores().create( repo, "adding npm hosted repo", HostedRepository.class );

        StoreKey storeKey = repo.getKey();
        assertThat( client.content().exists( storeKey, path ), equalTo( false ) );

        client.content().store( storeKey, path, content1 );
        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );

        client.content().store( storeKey, path, content2 );
        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );

        final InputStream is = client.content().get( storeKey, path );

        IndyObjectMapper mapper = new IndyObjectMapper( true );
        PackageMetadata reStoreMetadata = mapper.readValue( is, PackageMetadata.class );

        // versions map merging verification when re-publish
        Map<String, VersionMetadata> versions = reStoreMetadata.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 2 ) );
        assertThat( versions.get( "1.5.1" ).getVersion(), equalTo( "1.5.1" ) );
        assertThat( versions.get( "1.6.2" ).getVersion(), equalTo( "1.6.2" ) );

        is.close();
        content1.close();
        content2.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
