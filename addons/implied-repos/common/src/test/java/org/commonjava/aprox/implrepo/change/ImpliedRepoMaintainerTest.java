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
        storeDataManager.storeGroup( g, summary );

        final RemoteRepository repo1 = new RemoteRepository( "one", "http://www.foo.com/repo" );
        storeDataManager.storeRemoteRepository( repo1, summary );

        final RemoteRepository repo2 = new RemoteRepository( "one", "http://www.foo.com/repo" );
        storeDataManager.storeRemoteRepository( repo2, summary );

        metadataManager.addImpliedMetadata( repo1, Arrays.<ArtifactStore> asList( repo2 ) );

        g.addConstituent( repo1 );

        final ArtifactStorePreUpdateEvent event = new ArtifactStorePreUpdateEvent( ArtifactStoreUpdateType.UPDATE, g );
        maintainer.updateImpliedStores( event );

        assertThat( g.getConstituents()
                     .contains( repo2.getKey() ), equalTo( true ) );
    }

    @Test
    public void dontRemoveImpliedRepoWhenRepoRemovedFromGroup()
        throws Exception
    {
        final Group g = new Group( "test" );
        storeDataManager.storeGroup( g, summary );

        final RemoteRepository repo1 = new RemoteRepository( "one", "http://www.foo.com/repo" );
        storeDataManager.storeRemoteRepository( repo1, summary );

        final RemoteRepository repo2 = new RemoteRepository( "one", "http://www.foo.com/repo" );
        storeDataManager.storeRemoteRepository( repo2, summary );

        metadataManager.addImpliedMetadata( repo1, Arrays.<ArtifactStore> asList( repo2 ) );

        // Simulates removal of repo1...odd, I know, but since they post-process these updates, it's what the 
        // event observers would see.
        g.addConstituent( repo2 );

        final ArtifactStorePreUpdateEvent event = new ArtifactStorePreUpdateEvent( ArtifactStoreUpdateType.UPDATE, g );
        maintainer.updateImpliedStores( event );

        assertThat( g.getConstituents()
                     .contains( repo2.getKey() ), equalTo( true ) );
    }

}
