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
package org.commonjava.indy.implrepo.change;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.implrepo.conf.ImpliedRepoConfig;
import org.commonjava.indy.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.event.FileStorageEvent;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ImpliedRepositoryDetectorTest
{
    public static final class TestImpliedRepositoryDetector
            extends ImpliedRepositoryDetector
    {
        public TestImpliedRepositoryDetector( MavenPomReader pomReader, StoreDataManager storeManager,
                                              ImpliedRepoMetadataManager metadataManager, ImpliedRepoConfig config )
        {
            super( pomReader, storeManager, metadataManager, config );
        }

        @Override
        public String formatId( String id )
        {
            return super.formatId( id );
        }
    }

    private TestImpliedRepositoryDetector detector;

    private StoreDataManager storeManager;

    @Rule
    public GalleyMavenFixture fixture = new GalleyMavenFixture();

    private static final String GROUP_NAME = "group";

    private static final String REMOTE_NAME = "test";

    private ImpliedRepoMetadataManager metadataManager;

    private ChangeSummary summary;

    @Before
    public void setup()
        throws Exception
    {
        storeManager = new MemoryStoreDataManager(true);

        metadataManager =
            new ImpliedRepoMetadataManager( new IndyObjectMapper( true ) );

        final ImpliedRepoConfig config = new ImpliedRepoConfig();
        config.setEnabled( true );

        detector = new TestImpliedRepositoryDetector( fixture.getPomReader(), storeManager, metadataManager, config );

        summary = new ChangeSummary( ChangeSummary.SYSTEM_USER, "test setup" );

        RemoteRepository remote = new RemoteRepository( "test", "http://www.foo.com/repo" );
        Group group = new Group( "group", remote.getKey() );

        storeManager.storeArtifactStore( remote, summary, new EventMetadata() );
        storeManager.storeArtifactStore( group, summary, new EventMetadata() );
    }

    private RemoteRepository getRemote()
            throws IndyDataException
    {
        return storeManager.getRemoteRepository( REMOTE_NAME );
    }

    private Group getGroup()
            throws IndyDataException
    {
        return storeManager.getGroup( GROUP_NAME );
    }


    @Test
    public void idWithSpaceInIt_ConvertToDashes()
    {
        String in = "my id";
        String out = detector.formatId( in );
        assertThat( out, equalTo( "i-my-id" ) );
    }

    @Test
    public void idWithPlusInIt_ConvertToDashes()
    {
        String in = "my+id";
        String out = detector.formatId( in );
        assertThat( out, equalTo( "i-my-id" ) );
    }

    @Test
    public void addRepositoryFromPomStorageEvent()
        throws Exception
    {
        final String path = "/path/to/1/to-1.pom";
        final Transfer txfr = fixture.getCache()
                                     .getTransfer( new ConcreteResource( new RepositoryLocation( getRemote() ), path ) );

        final OutputStream out = txfr.openOutputStream( TransferOperation.UPLOAD, false );
        final InputStream in = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream( "one-repo.pom" );
        IOUtils.copy( in, out );
        IOUtils.closeQuietly( in );
        IOUtils.closeQuietly( out );

        final FileStorageEvent event = new FileStorageEvent( TransferOperation.DOWNLOAD, txfr, new EventMetadata() );
        detector.detectRepos( event );

        assertThat( storeManager.getRemoteRepository( "i-repo-one" ), notNullValue() );

        assertThat( getGroup().getConstituents()
                         .contains( new StoreKey( StoreType.remote, "i-repo-one" ) ), equalTo( true ) );
    }

    @Test
    public void addImpliedPluginRepositoryToNewGroup()
        throws Exception
    {
        final String path = "/path/to/1/to-1.pom";
        final Transfer txfr = fixture.getCache()
                                     .getTransfer( new ConcreteResource( new RepositoryLocation( getRemote() ), path ) );

        final OutputStream out = txfr.openOutputStream( TransferOperation.UPLOAD, false );
        final InputStream in = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream( "one-plugin-repo.pom" );
        IOUtils.copy( in, out );
        IOUtils.closeQuietly( in );
        IOUtils.closeQuietly( out );

        final FileStorageEvent event = new FileStorageEvent( TransferOperation.DOWNLOAD, txfr, new EventMetadata() );
        detector.detectRepos( event );

        assertThat( storeManager.getRemoteRepository( "i-repo-one" ), notNullValue() );

        assertThat( getGroup().getConstituents()
                         .contains( new StoreKey( StoreType.remote, "i-repo-one" ) ), equalTo( true ) );
    }

}
