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
package org.commonjava.indy.rest.util;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.IOUtils;
import org.commonjava.cdi.util.weft.PoolWeftExecutorService;
import org.commonjava.cdi.util.weft.WeftExecutorService;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.IndyLocationExpander;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.core.content.DefaultDownloadManager;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.nfc.NoOpNotFoundCache;
import org.commonjava.maven.galley.testing.core.transport.job.TestDownload;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DownloadManagerTest
{

    private DownloadManager downloader;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public GalleyMavenFixture fixture = new GalleyMavenFixture( tempFolder );

    private MemoryStoreDataManager data;

    private final ChangeSummary summary = new ChangeSummary( "test-user", "test" );

    @Before
    public void setupTest()
        throws Exception
    {
        data = new MemoryStoreDataManager( true );

        WeftExecutorService rescanService =
                        new PoolWeftExecutorService( "test-rescan-executor", (ThreadPoolExecutor) Executors.newCachedThreadPool(), 2, 10f, false, null, null );

        downloader = new DefaultDownloadManager( data, fixture.getTransferManager(), new IndyLocationExpander( data ),
                                                 null, new NoOpNotFoundCache(), rescanService );
    }

    @Test
    public void downloadOnePOMFromSingleRepository()
        throws Exception
    {
        final String content = "This is a test";
        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";
        
        final RemoteRepository repo = new RemoteRepository( MAVEN_PKG_KEY, "central", "http://repo.maven.apache.org/maven2" );
        fixture.getTransport()
               .registerDownload( new ConcreteResource( new RepositoryLocation( repo ), path ),
                                  new TestDownload( content.getBytes() ) );

        data.storeArtifactStore( repo, summary, false, true, new EventMetadata() );

        final Transfer stream = downloader.retrieve( repo, path, new EventMetadata() );
        final String downloaded = IOUtils.toString( stream.openInputStream() );

        assertThat( downloaded, equalTo( content ) );
    }

    @Test
    public void downloadOnePOMFromSecondRepositoryAfterDummyRepoFails()
        throws Exception
    {
        final RemoteRepository repo = new RemoteRepository( MAVEN_PKG_KEY, "dummy", "http://www.nowhere.com/" );

        final String content = "This is a test";
        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final RemoteRepository repo2 = new RemoteRepository( MAVEN_PKG_KEY, "central", "http://repo.maven.apache.org/maven2" );

        fixture.getTransport()
               .registerDownload( new ConcreteResource( new RepositoryLocation( repo2 ), path ),
                                  new TestDownload( content.getBytes() ) );


        data.storeArtifactStore( repo, summary, false, true, new EventMetadata() );
        data.storeArtifactStore( repo2, summary, false, true, new EventMetadata() );

        final List<ArtifactStore> repos = new ArrayList<ArtifactStore>();
        repos.add( repo );
        repos.add( repo2 );

        final Transfer stream = downloader.retrieveFirst( repos, path, new EventMetadata() );
        final String downloaded = IOUtils.toString( stream.openInputStream() );

        assertThat( downloaded, equalTo( content ) );
    }

}
