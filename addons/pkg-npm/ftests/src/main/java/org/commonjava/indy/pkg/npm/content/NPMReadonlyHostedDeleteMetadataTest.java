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
package org.commonjava.indy.pkg.npm.content;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.commonjava.indy.pkg.npm.model.VersionMetadata;
import org.commonjava.indy.util.ApplicationStatus;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * This case tests if metadata files can be deleted in a readonly hosted repo
 * when: <br />
 * <ul>
 *      <li>create a readonly hosted repo A and stores a tgz file and a stale metadata</li>
 *      <li>create a group G to include A</li>
 *      <li>get metadata from both A and G</li>
 *      <li>delete the metadata file from hosted repo A</li>
 *      <li>get the metadata file from A and G again</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the metadata file can be gotten from both A and G</li>
 *     <li>the metadata file can be deleted</li>
 *     <li>the metadata file is regenerated the second time with right content from both A and G</li>
 * </ul>
 */
public class NPMReadonlyHostedDeleteMetadataTest
                extends AbstractContentManagementTest
{
    private final IndyRawHttpModule httpModule = new IndyRawHttpModule();

    private final IndyObjectMapper mapper = new IndyObjectMapper( true );

    final String packagePath = "@babel/helper-validator-identifier";

    final String tarballPath = "@babel/helper-validator-identifier/-/helper-validator-identifier-7.10.4.tgz";

    final String staleMeta = "{\n" + "  \"dist-tags\" : { }\n" + "}";

    final String repoName = "test";

    final String groupName = "G";

    @Test
    public void test() throws Exception
    {
        // Prepare
        HostedRepository repo = new HostedRepository( NPM_PKG_KEY, repoName );
        repo = client.stores().create( repo, "adding npm hosted repo", HostedRepository.class );

        StoreKey storeKey = repo.getKey();
        client.content()
              .store( storeKey, tarballPath, readTestResourceAsStream( "helper-validator-identifier-7.10.4.tgz" ) );

        client.content().store( storeKey, packagePath, new ByteArrayInputStream( staleMeta.getBytes() ) );

        // Set readonly
        repo.setReadonly( true );
        client.stores().update( repo, "set read-only" );

        // Create group G
        Group group = new Group( NPM_PKG_KEY, groupName, repo.getKey() );
        client.stores().create( group, "adding npm group G", Group.class );

        // get from both A and G
        assertContent( repo, packagePath, staleMeta );
        assertContent( group, packagePath, staleMeta );

        // Extra case: delete non-metadata file is not allowed
        try
        {
            client.content().delete( storeKey, tarballPath );
        }
        catch ( IndyClientException e )
        {
            assertThat( e.getStatusCode(), equalTo( ApplicationStatus.METHOD_NOT_ALLOWED.code() ) );
        }

        // Delete metadata from repo A
        IndyClientHttp http = client.module( IndyRawHttpModule.class ).getHttp();
        http.deleteCache( client.content().contentPath( storeKey, packagePath ) + "/package.json" );

        assertExistence( repo, packagePath, false );
        assertExistence( group, packagePath, false );

        // Get metadata again
        try (InputStream is = client.content().get( storeKey, packagePath ))
        {
            //String actual = IOUtils.toString( is );
            //System.out.println( ">>>>\n" + actual );
            assertTarballUrl( is, "/api/content/npm/hosted/test/" + tarballPath );
        }

        try (InputStream is = client.content().get( group.getKey(), packagePath ))
        {
            //String actual = IOUtils.toString( is );
            //System.out.println( ">>>>\n" + actual );
            assertTarballUrl( is, "/api/content/npm/group/G/" + tarballPath );
        }
    }

    private void assertTarballUrl( InputStream is, String s ) throws IOException
    {
        PackageMetadata pkgMetadata = mapper.readValue( is, PackageMetadata.class );
        VersionMetadata versionMetadata = pkgMetadata.getVersions().get( "7.10.4" );
        assertNotNull( versionMetadata );
        String tarball = versionMetadata.getDist().getTarball();
        assertNotNull( tarball );
        assertTrue( tarball.contains( s ) );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.asList( httpModule );
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
