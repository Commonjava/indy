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
package org.commonjava.indy.implrepo.skim;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A pom in remote repo test with path p</li>
 *     <li>The pom contains declaration of a plugin repo r</li>
 *     <li>Group pub contains remote repo test</li>
 *     <li>No remote repo point to plugin repo r contained in group pub at first</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>Access pom through path p in group pub</li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>A new remote i point to plugin repo r will be added into group pub</li>
 *     <li>Remote test will have metatada "implied_stores" point to "remote:i"</li>
 *     <li>This remote i will have metatada "implied_by_stores" point to "remote:test"</li>
 * </ul>
 */
public class PomWithPluginRepoAddsRepoToGroupTest
    extends AbstractSkimFunctionalTest
{

    private static final String REPO = "i-repo-one";

    @Test
    public void skimPomForRepoAndAddIt() throws Exception
    {
        final PomRef ref =
            loadPom( "one-plugin-repo", Collections.singletonMap( "one-repo.url", server.formatUrl( REPO ) ) );

        server.expect( "HEAD", server.formatUrl( REPO, "/" ), 200, (String) null  );
        server.expect( server.formatUrl( TEST_REPO, ref.path ), 200, ref.pom );

        final StoreKey pubGroupKey = new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.group,
                                                   PUBLIC );

        final InputStream stream = client.content().get( pubGroupKey, ref.path );
        final String downloaded = IOUtils.toString( stream );
        IOUtils.closeQuietly( stream );
        
        assertThat( "SANITY: downloaded POM is wrong!", downloaded, equalTo( ref.pom ) );
        
        // sleep while event observer runs...
        System.out.println( "Waiting 5s for events to run." );
        Thread.sleep( 5000 );

        final Group g = client.stores().load( pubGroupKey, Group.class );
        final StoreKey remoteRepoKey = new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.remote, REPO );
        assertThat( "Group membership does not contain implied repository",
                    g.getConstituents().contains( remoteRepoKey ), equalTo( true ) );

        RemoteRepository r = client.stores()
                                   .load( new StoreKey( MavenPackageTypeDescriptor.MAVEN_PKG_KEY, StoreType.remote,
                                                        TEST_REPO ), RemoteRepository.class );

        String metadata = r.getMetadata( ImpliedRepoMetadataManager.IMPLIED_STORES );

        assertThat( "Reference to repositories implied by POMs in this repo is missing from metadata.",
                    metadata.contains( "remote:" + REPO ), equalTo( true ) );

        r = client.stores().load( remoteRepoKey, RemoteRepository.class );

        metadata = r.getMetadata( ImpliedRepoMetadataManager.IMPLIED_BY_STORES );

        assertThat( "Backref to repo with pom that implies this repo is missing from metadata.",
                    metadata.contains( "remote:" + TEST_REPO ), equalTo( true ) );
    }

}
