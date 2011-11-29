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
package org.commonjava.aprox.core.rest.util;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Repository;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Before;
import org.junit.Test;

public class FileManagerWeldTest
{

    private FileManager downloader;

    @Before
    public void setup()
    {
        WeldContainer weld = new Weld().initialize();
        downloader = weld.instance().select( DefaultFileManager.class ).get();
    }

    @Test
    public void downloadOnePOMFromSingleRepository()
        throws IOException
    {
        Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        File downloaded = downloader.download( repo, path );
        String pom = readFileToString( downloaded );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

    @Test
    public void downloadOnePOMFromSecondRepositoryAfterDummyRepoFails()
        throws IOException
    {
        Repository repo = new Repository( "dummy", "http://www.nowhere.com/" );
        Repository repo2 = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );

        String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        List<ArtifactStore> repos = new ArrayList<ArtifactStore>();
        repos.add( repo );
        repos.add( repo2 );

        File downloaded = downloader.downloadFirst( repos, path );
        assertThat( downloaded.exists(), equalTo( true ) );

        String pom = readFileToString( downloaded );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

}
