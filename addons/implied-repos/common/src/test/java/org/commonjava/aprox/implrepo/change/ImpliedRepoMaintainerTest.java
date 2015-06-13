/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.implrepo.change;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.aprox.change.event.ArtifactStoreUpdateType;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.aprox.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
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
        metadataManager = new ImpliedRepoMetadataManager( new AproxObjectMapper( true ) );

        final ImpliedRepoConfig config = new ImpliedRepoConfig();
        config.setEnabled( true );

        maintainer = new ImpliedRepoMaintainer( storeDataManager, metadataManager, config );
        summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" );
    }

    @Test
    public void addImpliedRepoWhenRepoAddedToGroup()
        throws Exception
    {
        final Group g = new Group( "test" );
        storeDataManager.storeArtifactStore( g, summary, new EventMetadata() );

        final RemoteRepository repo1 = new RemoteRepository( "one", "http://www.foo.com/repo" );
        storeDataManager.storeArtifactStore( repo1, summary, new EventMetadata() );

        final RemoteRepository repo2 = new RemoteRepository( "one", "http://www.foo.com/repo" );
        storeDataManager.storeArtifactStore( repo2, summary, new EventMetadata() );

        metadataManager.addImpliedMetadata( repo1, Arrays.<ArtifactStore> asList( repo2 ) );

        g.addConstituent( repo1 );

        final ArtifactStorePreUpdateEvent event =
            new ArtifactStorePreUpdateEvent( ArtifactStoreUpdateType.UPDATE, new EventMetadata(), g );
        maintainer.updateImpliedStores( event );

        assertThat( g.getConstituents()
                     .contains( repo2.getKey() ), equalTo( true ) );
    }

    @Test
    public void dontRemoveImpliedRepoWhenRepoRemovedFromGroup()
        throws Exception
    {
        final Group g = new Group( "test" );
        storeDataManager.storeArtifactStore( g, summary, new EventMetadata() );

        final RemoteRepository repo1 = new RemoteRepository( "one", "http://www.foo.com/repo" );
        storeDataManager.storeArtifactStore( repo1, summary, new EventMetadata() );

        final RemoteRepository repo2 = new RemoteRepository( "one", "http://www.foo.com/repo" );
        storeDataManager.storeArtifactStore( repo2, summary, new EventMetadata() );

        metadataManager.addImpliedMetadata( repo1, Arrays.<ArtifactStore> asList( repo2 ) );

        // Simulates removal of repo1...odd, I know, but since they post-process these updates, it's what the 
        // event observers would see.
        g.addConstituent( repo2 );

        final ArtifactStorePreUpdateEvent event =
            new ArtifactStorePreUpdateEvent( ArtifactStoreUpdateType.UPDATE, new EventMetadata(), g );
        maintainer.updateImpliedStores( event );

        assertThat( g.getConstituents()
                     .contains( repo2.getKey() ), equalTo( true ) );
    }

}
