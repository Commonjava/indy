/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.rest.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.commonjava.aprox.core.filer.DefaultFileManager;
import org.commonjava.aprox.filer.AproxLocationExpander;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.fixture.GalleyFixture;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Repository;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.util.logging.Log4jUtil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PathRetrieverTest
{

    private FileManager downloader;

    private File repoRoot;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private GalleyFixture fixture;

    private MemoryStoreDataManager data;

    @BeforeClass
    public static void logging()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Before
    public void setupTest()
        throws IOException
    {
        repoRoot = tempFolder.newFolder( "repository" );
        fixture = new GalleyFixture( repoRoot );
        data = new MemoryStoreDataManager();
        downloader = new DefaultFileManager( fixture.getTransfers(), new AproxLocationExpander( data ) );
    }

    @Test
    public void downloadOnePOMFromSingleRepository()
        throws Exception
    {
        final Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        data.storeRepository( repo );

        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final Transfer stream = downloader.retrieve( repo, path );
        final String pom = IOUtils.toString( stream.openInputStream() );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

    @Test
    public void downloadOnePOMFromSecondRepositoryAfterDummyRepoFails()
        throws Exception
    {
        final Repository repo = new Repository( "dummy", "http://www.nowhere.com/" );
        final Repository repo2 = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );

        data.storeRepository( repo );
        data.storeRepository( repo2 );

        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final List<ArtifactStore> repos = new ArrayList<ArtifactStore>();
        repos.add( repo );
        repos.add( repo2 );

        final Transfer stream = downloader.retrieveFirst( repos, path );
        final String pom = IOUtils.toString( stream.openInputStream() );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

}
