/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.rest.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.content.AproxLocationExpander;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.core.content.DefaultDownloadManager;
import org.commonjava.aprox.fixture.GalleyFixture;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.maven.galley.model.Transfer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PathRetrieverTest
{

    private DownloadManager downloader;

    private File repoRoot;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private GalleyFixture fixture;

    private MemoryStoreDataManager data;

    @Before
    public void setupTest()
        throws IOException
    {
        repoRoot = tempFolder.newFolder( "repository" );
        fixture = new GalleyFixture( repoRoot );
        data = new MemoryStoreDataManager();

        downloader = new DefaultDownloadManager( data, fixture.getTransfers(), new AproxLocationExpander( data ) );
    }

    @Test
    public void downloadOnePOMFromSingleRepository()
        throws Exception
    {
        final RemoteRepository repo = new RemoteRepository( "central", "http://repo.maven.apache.org/maven2/" );
        data.storeRemoteRepository( repo, "test" );

        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final Transfer stream = downloader.retrieve( repo, path );
        final String pom = IOUtils.toString( stream.openInputStream() );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

    @Test
    public void downloadOnePOMFromSecondRepositoryAfterDummyRepoFails()
        throws Exception
    {
        final RemoteRepository repo = new RemoteRepository( "dummy", "http://www.nowhere.com/" );
        final RemoteRepository repo2 = new RemoteRepository( "central", "http://repo.maven.apache.org/maven2/" );

        data.storeRemoteRepository( repo, "test" );
        data.storeRemoteRepository( repo2, "test" );

        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final List<ArtifactStore> repos = new ArrayList<ArtifactStore>();
        repos.add( repo );
        repos.add( repo2 );

        final Transfer stream = downloader.retrieveFirst( repos, path );
        final String pom = IOUtils.toString( stream.openInputStream() );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

}
