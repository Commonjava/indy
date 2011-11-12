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
package org.commonjava.aprox.core.live.rest.util;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.fixture.ProxyConfigProvider;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.rest.util.FileManager;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.web.test.AbstractRESTCouchTest;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class FileManagerLiveTest
    extends AbstractRESTCouchTest
{

    @Deployment
    public static WebArchive createWar()
    {
        return new TestWarArchiveBuilder( FileManagerLiveTest.class ).withExtraClasses( ProxyConfigProvider.class )
                                                                     .build();
    }

    @Inject
    private FileManager downloader;

    @Inject
    protected ProxyDataManager proxyManager;

    @Inject
    @AproxData
    private CouchManager couch;

    @Test
    public void downloadOnePOMFromSingleRepository()
        throws IOException
    {
        final Repository repo = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );
        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final File downloaded = downloader.download( repo, path );
        final String pom = readFileToString( downloaded );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

    @Test
    public void downloadOnePOMFromSecondRepositoryAfterDummyRepoFails()
        throws IOException
    {
        final Repository repo = new Repository( "dummy", "http://www.nowhere.com/" );
        final Repository repo2 = new Repository( "central", "http://repo1.maven.apache.org/maven2/" );

        final String path = "/org/apache/maven/maven-model/3.0.3/maven-model-3.0.3.pom";

        final List<ArtifactStore> repos = new ArrayList<ArtifactStore>();
        repos.add( repo );
        repos.add( repo2 );

        final File downloaded = downloader.downloadFirst( repos, path );
        assertThat( downloaded.exists(), equalTo( true ) );

        final String pom = readFileToString( downloaded );

        assertThat( pom.contains( "<artifactId>maven-model</artifactId>" ), equalTo( true ) );
    }

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        proxyManager.install();
    }

    @After
    public final void teardownAProxLiveTest()
        throws Exception
    {
        couch.dropDatabase();
    }

    @Override
    protected CouchManager getCouchManager()
    {
        return couch;
    }

}
