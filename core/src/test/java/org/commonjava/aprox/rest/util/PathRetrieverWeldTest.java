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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.live.fixture.ProxyConfigProvider;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Repository;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PathRetrieverWeldTest
{

    private FileManager downloader;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @SuppressWarnings( "serial" )
    @Before
    public void setup()
    {
        final File storage = temp.newFolder( "storage" );

        final Properties p = System.getProperties();
        p.remove( ProxyConfigProvider.REPO_ROOT_DIR );
        p.setProperty( ProxyConfigProvider.REPO_ROOT_DIR, storage.getAbsolutePath() );
        System.setProperties( p );

        final WeldContainer weld = new Weld().initialize();
        downloader = weld.instance()
                         .select( FileManager.class )
                         .get();
    }

    @Test
    public void downloadOnePOMFromSingleRepository()
        throws Exception
    {
        final Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final StorageItem stream = downloader.retrieve( repo, path );
        final String pom = IOUtils.toString( stream.openInputStream() );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

    @Test
    public void downloadOnePOMFromSecondRepositoryAfterDummyRepoFails()
        throws Exception
    {
        final Repository repo = new Repository( "dummy", "http://www.nowhere.com/" );
        final Repository repo2 = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );

        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final List<ArtifactStore> repos = new ArrayList<ArtifactStore>();
        repos.add( repo );
        repos.add( repo2 );

        final StorageItem stream = downloader.retrieveFirst( repos, path );
        final String pom = IOUtils.toString( stream.openInputStream() );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

}
