package org.commonjava.aprox.implrepo.change;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.aprox.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.model.galley.RepositoryLocation;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.testing.core.ApiFixture;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ImpliedRepositoryDetectorTest
{

    private ImpliedRepositoryDetector detector;

    private StoreDataManager storeManager;

    @Rule
    public GalleyMavenFixture fixture = new GalleyMavenFixture( new ApiFixture() );

    private RemoteRepository remote;

    private Group group;

    private ImpliedRepoMetadataManager metadataManager;

    private ChangeSummary summary;

    @Before
    public void setup()
        throws Exception
    {
        fixture.initMissingComponents();

        storeManager = new MemoryStoreDataManager();

        metadataManager =
            new ImpliedRepoMetadataManager( new AproxObjectMapper( true ) );

        final ImpliedRepoConfig config = new ImpliedRepoConfig();
        config.setEnabled( true );

        detector = new ImpliedRepositoryDetector( fixture.getPomReader(), storeManager, metadataManager, config );

        summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" );

        remote = new RemoteRepository( "test", "http://www.foo.com/repo" );
        group = new Group( "group", remote.getKey() );

        storeManager.storeRemoteRepository( remote, summary );
        storeManager.storeGroup( group, summary );
    }

    @Test
    public void addRepositoryFromPomStorageEvent()
        throws Exception
    {
        final String path = "/path/to/1/to-1.pom";
        final Transfer txfr = fixture.getCache()
                                     .getTransfer( new ConcreteResource( new RepositoryLocation( remote ), path ) );

        final OutputStream out = txfr.openOutputStream( TransferOperation.UPLOAD, false );
        final InputStream in = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream( "one-repo.pom" );
        IOUtils.copy( in, out );
        IOUtils.closeQuietly( in );
        IOUtils.closeQuietly( out );

        final FileStorageEvent event = new FileStorageEvent( TransferOperation.DOWNLOAD, txfr );
        detector.detectRepos( event );

        assertThat( storeManager.getRemoteRepository( "repo-one" ), notNullValue() );

        assertThat( group.getConstituents()
                         .contains( new StoreKey( StoreType.remote, "repo-one" ) ), equalTo( true ) );
    }

    @Test
    public void addImpliedPluginRepositoryToNewGroup()
        throws Exception
    {
        final String path = "/path/to/1/to-1.pom";
        final Transfer txfr = fixture.getCache()
                                     .getTransfer( new ConcreteResource( new RepositoryLocation( remote ), path ) );

        final OutputStream out = txfr.openOutputStream( TransferOperation.UPLOAD, false );
        final InputStream in = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream( "one-plugin-repo.pom" );
        IOUtils.copy( in, out );
        IOUtils.closeQuietly( in );
        IOUtils.closeQuietly( out );

        final FileStorageEvent event = new FileStorageEvent( TransferOperation.DOWNLOAD, txfr );
        detector.detectRepos( event );

        assertThat( storeManager.getRemoteRepository( "repo-one" ), notNullValue() );

        assertThat( group.getConstituents()
                         .contains( new StoreKey( StoreType.remote, "repo-one" ) ), equalTo( true ) );
    }

}
