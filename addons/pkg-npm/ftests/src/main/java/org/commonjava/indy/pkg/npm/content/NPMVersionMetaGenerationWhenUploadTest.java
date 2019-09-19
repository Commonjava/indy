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

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.commonjava.indy.pkg.npm.model.VersionMetadata;
import org.junit.Test;

import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * This case tests the version meta generation for a project's one version upload
 * when: <br />
 * <ul>
 *      <li>creates a hosted repo</li>
 *      <li>stores the project's package.json in the hosted repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the version meta file can be generated successfully</li>
 * </ul>
 */
public class NPMVersionMetaGenerationWhenUploadTest
        extends AbstractContentManagementTest
{
    @Test
    public void test()
            throws Exception
    {
        final String content = IOUtils.toString(
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.5.1.json" ) );

        final String path = "jquery";
        final String versionPath = "jquery/1.5.1";

        final String repoName = "test-hosted";
        HostedRepository repo = new HostedRepository( NPM_PKG_KEY, repoName );

        repo = client.stores().create( repo, "adding npm hosted repo", HostedRepository.class );

        StoreKey storeKey = repo.getKey();
        assertThat( client.content().exists( storeKey, path ), equalTo( false ) );

        client.content().store( storeKey, path, IOUtils.toInputStream( content ) );

        assertThat( client.content().exists( storeKey, path ), equalTo( true ) );
        assertThat( client.content().exists( storeKey, versionPath ), equalTo( true ) );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        PackageMetadata packageMetadata = mapper.readValue( content, PackageMetadata.class );
        VersionMetadata versionMetadata = packageMetadata.getVersions().get( "1.5.1" );
        String name = versionMetadata.getName();
        String desc = versionMetadata.getDescription();
        String version = versionMetadata.getVersion();

        InputStream stream = client.content().get( storeKey, versionPath );

        VersionMetadata actualVersionMeta = mapper.readValue( IOUtils.toString( stream ), VersionMetadata.class );
        String actualName = actualVersionMeta.getName();
        String actualDesc = actualVersionMeta.getDescription();
        String actualVersion = actualVersionMeta.getVersion();

        assertEquals( name, actualName );
        assertEquals( desc, actualDesc );
        assertEquals( version, actualVersion );

        stream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
