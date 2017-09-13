/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import org.commonjava.indy.ftest.core.category.EventDependent;
import org.commonjava.indy.ftest.core.category.TimingDependent;
import org.commonjava.indy.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.InputStream;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class PomWithRepoAddsRepoToGroupTest
    extends AbstractSkimFunctionalTest
{

    private static final String REPO = "i-repo-one";

    @Test
    public void skimPomForRepoAndAddIt() throws Exception
    {
        final PomRef ref = loadPom( "one-repo", Collections.singletonMap( "one-repo.url", server.formatUrl( REPO ) ) );
        
        server.expect( server.formatUrl( TEST_REPO, ref.path ), 200, ref.pom );

        final InputStream stream = client.content()
                                         .get( StoreType.group, PUBLIC, ref.path );
        final String downloaded = IOUtils.toString( stream );
        IOUtils.closeQuietly( stream );
        
        assertThat( "SANITY: downloaded POM is wrong!", downloaded, equalTo( ref.pom ) );
        
        // sleep while event observer runs...
        System.out.println( "Waiting 5s for events to run." );
        Thread.sleep( 5000 );

        final Group g = client.stores().load( StoreType.group, PUBLIC, Group.class );
        assertThat( "Group membership does not contain implied repository",
                    g.getConstituents()
                     .contains( new StoreKey( StoreType.remote, REPO ) ), equalTo( true ) );

        RemoteRepository r = client.stores()
                                         .load( StoreType.remote, TEST_REPO, RemoteRepository.class );

        String metadata = r.getMetadata( ImpliedRepoMetadataManager.IMPLIED_STORES );

        assertThat( "Reference to repositories implied by POMs in this repo is missing from metadata.",
                    metadata.contains( "remote:" + REPO ), equalTo( true ) );

        r = client.stores()
                  .load( StoreType.remote, REPO, RemoteRepository.class );

        metadata = r.getMetadata( ImpliedRepoMetadataManager.IMPLIED_BY_STORES );

        assertThat( "Backref to repo with pom that implies this repo is missing from metadata.",
                    metadata.contains( "remote:" + TEST_REPO ), equalTo( true ) );
    }

}
