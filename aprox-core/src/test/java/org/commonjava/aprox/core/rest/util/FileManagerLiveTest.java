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
package org.commonjava.aprox.core.rest.util;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.commonjava.aprox.core.change.MavenMetadataUploadListener;
import org.commonjava.aprox.core.conf.DefaultProxyConfiguration;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.data.ProxyAppDescription;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.fixture.AProxTestPropertiesProvider;
import org.commonjava.aprox.core.fixture.ProxyConfigProvider;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.aprox.core.inject.AproxDataProviders;
import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.web.test.AbstractRESTCouchTest;
import org.commonjava.web.test.fixture.TestData;
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

    @Deployment
    public static WebArchive createWar()
    {
        TestWarArchiveBuilder builder =
            new TestWarArchiveBuilder( AProxTestPropertiesProvider.class );

        builder.withExtraClasses( FileManager.class, TLRepositoryCredentialsProvider.class,
                                  AProxTestPropertiesProvider.class, ProxyConfiguration.class,
                                  DefaultProxyConfiguration.class );

        builder.withExtraPackages( true, AproxDataProviders.class.getPackage(),
                                   ProxyConfigProvider.class.getPackage(),
                                   TestData.class.getPackage(), ArtifactStore.class.getPackage(),
                                   ProxyDataManager.class.getPackage(),
                                   CouchDBConfiguration.class.getPackage(), // Isn't this already in there??
                                   MavenMetadataUploadListener.class.getPackage() );

        builder.withStandardPackages();
        builder.withLog4jProperties();
        builder.withAllStandards();
        builder.withApplication( new ProxyAppDescription() );
        // builder.withApplication( new UserAppDescription() );

        WebArchive archive = builder.build();

        String basedir = System.getProperty( "basedir", "." );
        archive.addAsWebInfResource( new File( basedir, "src/test/resources/META-INF/beans.xml" ) );

        return archive;
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
