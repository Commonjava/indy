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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.commonjava.indy.pkg.npm.model.VersionMetadata;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

/**
 * This case tests if package.json version can be merged correctly for group repo
 * when: <br />
 * <ul>
 *      <li>create remote and hosted repos and expect two package.json files in them</li>
 *      <li>create two group repos, one contains the remote member and the other contains hosted+group members</li>
 *      <li>create another group repo, different sort, group+hosted members</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the version in the merged file can be retrieved correctly for the three groups</li>
 * </ul>
 */
public class NPMGroupVersionContentMergeRetrieveTest
        extends AbstractContentManagementTest
{

    private static final String NPMJS_REMOTE = "npmjs";

    private static final String SHARED_IMPORTS_HOSTED = "shared-imports";

    private static final String PUBLIC_GROUP = "public";

    private static final String SHARED_IMPORTS_PUBLIC_GROUP = "shared-imports+public";

    private static final String PUBLIC_SHARED_IMPORTS_GROUP = "public+shared-imports";

    private static final String PATH = "kie";

    @Test
    public void test()
            throws Exception
    {
        final String REMOTE_CONTENT = IOUtils.toString(
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-remote.json" ) );
        server.expect( server.formatUrl( NPMJS_REMOTE, PATH ), 200,
                       new ByteArrayInputStream( REMOTE_CONTENT.getBytes() ) );
        final RemoteRepository npmjsRemote =
                new RemoteRepository( NPM_PKG_KEY, NPMJS_REMOTE, server.formatUrl( NPMJS_REMOTE ) );
        client.stores().create( npmjsRemote, "adding npm remote repo", RemoteRepository.class );

        final Group publicGroup = new Group( NPM_PKG_KEY, PUBLIC_GROUP, npmjsRemote.getKey() );
        client.stores().create( publicGroup, "adding npm group repo", Group.class );
        System.out.printf( "\n\n-------Group constituents are:\n  %s\n\n",
                           StringUtils.join( publicGroup.getConstituents(), "\n  " ) );

        final InputStream HOSTED_CONTENT =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-hosted.json" );
        final HostedRepository sharedImportHosted = new HostedRepository( NPM_PKG_KEY, SHARED_IMPORTS_HOSTED );
        client.stores().create( sharedImportHosted, "adding npm hosted repo", HostedRepository.class );
        client.content().store( sharedImportHosted.getKey(), PATH, HOSTED_CONTENT );

        final Group sharedGroup = new Group( NPM_PKG_KEY, SHARED_IMPORTS_PUBLIC_GROUP, sharedImportHosted.getKey(),
                                             publicGroup.getKey() );
        client.stores().create( sharedGroup, "adding npm group repo", Group.class );
        System.out.printf( "\n\n-------Group constituents are:\n  %s\n\n",
                           StringUtils.join( sharedGroup.getConstituents(), "\n  " ) );

        final InputStream remoteInput = client.content().get( npmjsRemote.getKey(), PATH );
        final InputStream hostedInput = client.content().get( sharedImportHosted.getKey(), PATH );
        final InputStream publicGroupInput = client.content().get( publicGroup.getKey(), PATH );
        final InputStream sharedGroupInput = client.content().get( sharedGroup.getKey(), PATH );

        assertThat( remoteInput, notNullValue() );
        assertThat( hostedInput, notNullValue() );
        assertThat( publicGroupInput, notNullValue() );
        assertThat( sharedGroupInput, notNullValue() );

        IndyObjectMapper mapper = new IndyObjectMapper( true );
        PackageMetadata mergedPublic = mapper.readValue( IOUtils.toString( publicGroupInput ), PackageMetadata.class );
        PackageMetadata mergedShared = mapper.readValue( IOUtils.toString( sharedGroupInput ), PackageMetadata.class );

        // This public group version metadata in package metadata will follow npmjs remote
        VersionMetadata publicVersion = mergedPublic.getVersions().get( "0.2.2" );
        assertThat( publicVersion, notNullValue() );
        assertThat( publicVersion.getBin().toString(), equalTo( "{locktt=dist/index.js}" ) );
        assertThat( publicVersion.getMaintainers().size(), equalTo( 4 ) );
        assertThat(
                publicVersion.getDist().getTarball().contains( "lock-treatment-tool/-/lock-treatment-tool-0.2.2.tgz" ),
                equalTo( true ) );
        assertThat( publicVersion.getDist().getShasum(), equalTo( "5fa2f797f12176f0ae6c3ac993f7fa75aa1bf92e" ) );
        assertThat( publicVersion.getDist().getSignatures(), notNullValue() );
        assertThat( publicVersion.getDist().getIntegrity(), notNullValue() );
        assertThat( publicVersion.getDist().getNpmSignature(), notNullValue() );
        assertThat( publicVersion.getDist().getFileCount(), equalTo( 6 ) );
        assertThat( publicVersion.getDist().getUnpackedSize(), equalTo( 300837L ) );
        assertThat( publicVersion.getHomepage(), equalTo( "https://github.com/kiegroup/lock-treatment-tool" ) );
        assertThat( publicVersion.getPri(), equalTo( false ) );
        assertThat( publicVersion.getNodeVersion(), equalTo( "12.22.12" ) );
        assertThat( publicVersion.getNpmVersion(), equalTo( "6.14.16" ) );
        assertThat( publicVersion.getNpmUser().getName(), equalTo( "ginxo" ) );
        assertThat( publicVersion.getHasShrinkwrap(), equalTo( true ) );
        assertThat( publicVersion.getAuthor().getName(), equalTo( "Enrique Mingorance Cano" ) );
        assertThat( publicVersion.getRepository().getUrl(),
                    equalTo( "git+ssh://git@github.com/kiegroup/lock-treatment-tool.git" ) );
        assertNull( publicVersion.getFiles() );

        // This shared+public group version metadata in package metadata will accept the first one coming to the group: shared-imports hosted
        VersionMetadata mergedVersion = mergedShared.getVersions().get( "0.2.2" );
        assertThat( mergedVersion, notNullValue() );
        assertNull( mergedVersion.getBin() );
        assertNull( mergedVersion.getMaintainers() );
        assertThat(
                mergedVersion.getDist().getTarball().contains( "lock-treatment-tool/-/lock-treatment-tool-0.2.2.tgz" ),
                equalTo( true ) );
        assertNull( mergedVersion.getDist().getShasum() );
        assertNull( mergedVersion.getDist().getSignatures() );
        assertNull( mergedVersion.getDist().getIntegrity() );
        assertNull( mergedVersion.getDist().getNpmSignature() );
        assertNull( mergedVersion.getDist().getFileCount() );
        assertNull( mergedVersion.getDist().getUnpackedSize() );
        assertThat( mergedVersion.getHomepage(), equalTo( "https://github.com/kiegroup/lock-treatment-tool" ) );
        assertNull( mergedVersion.getPri() );
        assertNull( mergedVersion.getNodeVersion() );
        assertNull( mergedVersion.getNpmVersion() );
        assertNull( mergedVersion.getNpmUser() );
        assertNull( mergedVersion.getHasShrinkwrap() );
        assertThat( mergedVersion.getAuthor().getName(), equalTo( "Enrique Mingorance Cano <emingora@redhat.com>" ) );
        assertThat( mergedVersion.getRepository().getUrl(), equalTo( "git@github.com:kiegroup/lock-treatment-tool" ) );
        assertThat( mergedVersion.getFiles().size(), equalTo( 1 ) );

        // Add members to public+shared group 2, the sort is different from the first shared+public group,
        // the version metadata in package metadata will accept the first one coming to the group: public group
        final Group sharedGroup2 = new Group( NPM_PKG_KEY, PUBLIC_SHARED_IMPORTS_GROUP, publicGroup.getKey(),
                                              sharedImportHosted.getKey() );
        client.stores().create( sharedGroup2, "adding npm group repo", Group.class );
        System.out.printf( "\n\n-------Group constituents are:\n  %s\n\n",
                           StringUtils.join( sharedGroup2.getConstituents(), "\n  " ) );
        final InputStream sharedGroupInput2 = client.content().get( sharedGroup2.getKey(), PATH );
        PackageMetadata mergedShared2 =
                mapper.readValue( IOUtils.toString( sharedGroupInput2 ), PackageMetadata.class );
        VersionMetadata mergedVersion2 = mergedShared2.getVersions().get( "0.2.2" );
        assertThat( mergedVersion2, notNullValue() );
        assertThat( mergedVersion2.getBin().toString(), equalTo( "{locktt=dist/index.js}" ) );
        assertThat( mergedVersion2.getMaintainers().size(), equalTo( 4 ) );
        assertThat(
                mergedVersion2.getDist().getTarball().contains( "lock-treatment-tool/-/lock-treatment-tool-0.2.2.tgz" ),
                equalTo( true ) );
        assertThat( mergedVersion2.getDist().getShasum(), equalTo( "5fa2f797f12176f0ae6c3ac993f7fa75aa1bf92e" ) );
        assertThat( mergedVersion2.getDist().getSignatures(), notNullValue() );
        assertThat( mergedVersion2.getDist().getIntegrity(), notNullValue() );
        assertThat( mergedVersion2.getDist().getNpmSignature(), notNullValue() );
        assertThat( mergedVersion2.getDist().getFileCount(), equalTo( 6 ) );
        assertThat( mergedVersion2.getDist().getUnpackedSize(), equalTo( 300837L ) );
        assertThat( mergedVersion2.getHomepage(), equalTo( "https://github.com/kiegroup/lock-treatment-tool" ) );
        assertThat( mergedVersion2.getPri(), equalTo( false ) );
        assertThat( mergedVersion2.getNodeVersion(), equalTo( "12.22.12" ) );
        assertThat( mergedVersion2.getNpmVersion(), equalTo( "6.14.16" ) );
        assertThat( mergedVersion2.getNpmUser().getName(), equalTo( "ginxo" ) );
        assertThat( mergedVersion2.getHasShrinkwrap(), equalTo( true ) );
        assertThat( mergedVersion2.getAuthor().getName(), equalTo( "Enrique Mingorance Cano" ) );
        assertThat( mergedVersion2.getRepository().getUrl(),
                    equalTo( "git+ssh://git@github.com/kiegroup/lock-treatment-tool.git" ) );
        assertNull( mergedVersion2.getFiles() );

        //        final String jsonResult = mapper.writeValueAsString( mergedShared );
        remoteInput.close();
        hostedInput.close();
        publicGroupInput.close();
        sharedGroupInput.close();
        sharedGroupInput2.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
