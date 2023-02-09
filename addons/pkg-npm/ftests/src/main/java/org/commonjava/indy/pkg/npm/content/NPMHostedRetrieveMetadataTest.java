/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This case tests if metadata can be retrieved correctly in a hosted repo
 * when: <br />
 * <ul>
 *      <li>creates a hosted repo</li>
 *      <li>stores a .tgz file in hosted repo</li>
 *      <li>retrieve the package metadata in the hosted repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the metadata can be retrieved successfully</li>
 * </ul>
 */
public class NPMHostedRetrieveMetadataTest
                extends AbstractContentManagementTest
{
    @Test
    public void test() throws Exception
    {

        final String packagePath = "@babel/helper-validator-identifier";
        final String tarballPath = "@babel/helper-validator-identifier/-/helper-validator-identifier-7.10.4.tgz";

        final HostedRepository hostedRepository = new HostedRepository( NPM_PKG_KEY, STORE );
        final StoreKey storeKey = hostedRepository.getKey();

        client.stores().create( hostedRepository, "adding npm hosted repo", HostedRepository.class );

        client.content()
              .store( storeKey, tarballPath, readTestResourceAsStream( "helper-validator-identifier-7.10.4.tgz" ) );

        final InputStream packageStream = client.content().get( storeKey, packagePath );
        assertThat( packageStream, notNullValue() );

        IndyObjectMapper mapper = new IndyObjectMapper( true );
        PackageMetadata pkgMetadata = mapper.readValue( packageStream, PackageMetadata.class );
        VersionMetadata versionMetadata = pkgMetadata.getVersions().get( "7.10.4" );
        assertNotNull( versionMetadata );

        // e.g., http://127.0.0.1:8389/api/content/npm/hosted/test/@babel/helper-validator-identifier/-/helper-validator-identifier-7.10.4.tgz
        String expected = fixture.getUrl() + "content/npm/hosted/" + STORE + "/" + tarballPath;

        assertEquals( expected, versionMetadata.getDist().getTarball() );
        packageStream.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
