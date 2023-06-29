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
package org.commonjava.indy.folo.ftest.content.admin;

import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.folo.client.IndyFoloAdminClientModule;
import org.commonjava.indy.folo.client.IndyFoloContentClientModule;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.BatchDeleteRequest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.commonjava.indy.pkg.npm.model.VersionMetadata;
import org.junit.Test;

import java.io.InputStream;
import java.util.*;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>HostedRepository and the NPM packages with two versions</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Store the package with version 1.5.1 through the FOLO id into the repository</li>
 *     <li>Store the package with version 1.6.2 without FOLO id into the repository</li>
 *     <li>Seal the record</li>
 *     <li>Specifying the FOLO id to batch delete the artifacts from the repository</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>The package with version 1.5.1 does not exist in the repository</li>
 *     <li>The package with version 1.6.2 still exists in the repository</li>
 * </ul>
 */
public class BatchDeletionOfNPMPackagesTest extends AbstractContentManagementTest
{

    @Test
    public void test() throws Exception
    {

        final String trackingId = newName();

        final InputStream content1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.5.1.json" );

        final InputStream content2 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.6.2.json" );

        final String path = "jquery";

        final String firstTarballPath = "jquery/-/jquery-1.5.1.tgz";
        final String firstVersionPath = "jquery/1.5.1";

        final String secondTarballPath = "jquery/-/jquery-1.6.2.tgz";
        final String secondVersionPath = "jquery/1.6.2";

        HostedRepository repo = client.stores().create(new HostedRepository(NPM_PKG_KEY, STORE), "adding npm hosted repo", HostedRepository.class);

        StoreKey storeKey = repo.getKey();

        client.module( IndyFoloContentClientModule.class ).store( trackingId, storeKey, path, content1 );
        client.content().store( storeKey, path, content2 );

        IndyFoloAdminClientModule adminModule = client.module( IndyFoloAdminClientModule.class );
        boolean success = adminModule.sealTrackingRecord( trackingId );
        assertTrue( success );

        final InputStream is = client.content().get( storeKey, path );

        IndyObjectMapper mapper = new IndyObjectMapper( true );
        PackageMetadata packageMetadata = mapper.readValue( is, PackageMetadata.class );

        Map<String, VersionMetadata> versions = packageMetadata.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 2 ) );
        assertThat( versions.get( "1.5.1" ).getVersion(), equalTo( "1.5.1" ) );
        assertThat( versions.get( "1.6.2" ).getVersion(), equalTo( "1.6.2" ) );

        assertThat( client.content().exists( storeKey, firstTarballPath ), equalTo( true ) );
        assertThat( client.content().exists( storeKey, firstVersionPath ), equalTo( true ) );

        assertThat( client.content().exists( storeKey, secondTarballPath ), equalTo( true ) );
        assertThat( client.content().exists( storeKey, secondVersionPath ), equalTo( true ) );

        content1.close();
        content2.close();

        BatchDeleteRequest request = new BatchDeleteRequest();
        request.setStoreKey( storeKey );
        request.setTrackingID( trackingId );

        adminModule.deleteFilesFromStoreByTrackingID( request );

        final InputStream is_2 = client.content().get( storeKey, path );

        packageMetadata = mapper.readValue( is_2, PackageMetadata.class );

        versions = packageMetadata.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 1 ) );
        assertThat( versions.get( "1.6.2" ).getVersion(), equalTo( "1.6.2" ) );

        assertThat( client.content().exists( storeKey, firstTarballPath ), equalTo( false ) );
        assertThat( client.content().exists( storeKey, firstVersionPath ), equalTo( false ) );

        assertThat( client.content().exists( storeKey, secondTarballPath ), equalTo( true ) );
        assertThat( client.content().exists( storeKey, secondVersionPath ), equalTo( true ) );

    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Arrays.asList( new IndyFoloContentClientModule(), new IndyFoloAdminClientModule() );
    }
}
