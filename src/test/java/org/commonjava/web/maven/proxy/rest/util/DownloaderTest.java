/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.web.maven.proxy.rest.util;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.commonjava.web.maven.proxy.conf.DefaultProxyConfiguration;
import org.commonjava.web.maven.proxy.model.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DownloaderTest
{

    private Downloader downloader;

    private DefaultProxyConfiguration config;

    private File repoRoot;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setupTest()
        throws IOException
    {
        repoRoot = tempFolder.newFolder( "repository" );

        config = new DefaultProxyConfiguration();
        config.setRepositoryRootDirectory( repoRoot );

        downloader = new Downloader( config );
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

        List<Repository> repos = new ArrayList<Repository>();
        repos.add( repo );
        repos.add( repo2 );

        File downloaded = downloader.downloadFirst( repos, path );
        assertThat( downloaded.exists(), equalTo( true ) );

        String pom = readFileToString( downloaded );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

}
