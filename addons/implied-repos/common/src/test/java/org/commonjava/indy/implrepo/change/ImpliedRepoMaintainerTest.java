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
package org.commonjava.indy.implrepo.change;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.indy.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.maven.galley.event.EventMetadata;
import org.junit.Before;
import org.junit.Test;

public class ImpliedRepoMaintainerTest
{

    private ImpliedRepoMaintainer maintainer;

    private StoreDataManager storeDataManager;

    private ChangeSummary summary;

    private ImpliedRepoMetadataManager metadataManager;

    @Before
    public void setup()
    {
        storeDataManager = new MemoryStoreDataManager(true);
        metadataManager = new ImpliedRepoMetadataManager( new IndyObjectMapper( true ) );

        final ImpliedRepoConfig config = new ImpliedRepoConfig();
        config.setEnabled( true );

        maintainer = new ImpliedRepoMaintainer( storeDataManager, metadataManager, config );
        summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" );
    }

    @Test
    public void addImpliedRepoWhenRepoAddedToGroup()
        throws Exception
    {
        final Group g = new Group( MAVEN_PKG_KEY, "test" );
        storeDataManager.storeArtifactStore( g, summary, false, true, new EventMetadata() );

        final RemoteRepository repo1 = new RemoteRepository( MAVEN_PKG_KEY, "one", "http://www.foo.com/repo" );
        storeDataManager.storeArtifactStore( repo1, summary, false, true, new EventMetadata() );

        final RemoteRepository repo2 = new RemoteRepository( MAVEN_PKG_KEY, "one", "http://www.foo.com/repo" );
        storeDataManager.storeArtifactStore( repo2, summary, false, true, new EventMetadata() );

        metadataManager.addImpliedMetadata( repo1, Collections.singletonList( repo2 ) );

        g.addConstituent( repo1 );

        final ArtifactStorePreUpdateEvent event =
            new ArtifactStorePreUpdateEvent( ArtifactStoreUpdateType.UPDATE, new EventMetadata(), Collections.singletonMap( g.copyOf(), g ) );
        maintainer.updateImpliedStores( event );

        assertThat( g.getConstituents()
                     .contains( repo2.getKey() ), equalTo( true ) );
    }

    @Test
    public void dontRemoveImpliedRepoWhenRepoRemovedFromGroup()
        throws Exception
    {
        final Group g = new Group( MAVEN_PKG_KEY, "test" );
        storeDataManager.storeArtifactStore( g, summary, false, true, new EventMetadata() );

        final RemoteRepository repo1 = new RemoteRepository( MAVEN_PKG_KEY, "one", "http://www.foo.com/repo" );
        storeDataManager.storeArtifactStore( repo1, summary, false, true, new EventMetadata() );

        final RemoteRepository repo2 = new RemoteRepository( MAVEN_PKG_KEY, "one", "http://www.foo.com/repo" );
        storeDataManager.storeArtifactStore( repo2, summary, false, true, new EventMetadata() );

        metadataManager.addImpliedMetadata( repo1, Collections.singletonList( repo2 ) );

        // Simulates removal of repo1...odd, I know, but since they post-process these updates, it's what the 
        // event observers would see.
        g.addConstituent( repo2 );

        final ArtifactStorePreUpdateEvent event =
                new ArtifactStorePreUpdateEvent( ArtifactStoreUpdateType.UPDATE, new EventMetadata(),
                                                 Collections.singletonMap( g.copyOf(), g ) );
        maintainer.updateImpliedStores( event );

        assertThat( g.getConstituents()
                     .contains( repo2.getKey() ), equalTo( true ) );
    }

}
