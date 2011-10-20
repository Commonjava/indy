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
package org.commonjava.aprox.sec;

import static org.apache.commons.io.IOUtils.copy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;

import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.cjtest.fixture.TestUserManagerConfigProducer;
import org.codehaus.plexus.util.Os;
import org.commonjava.aprox.core.change.MavenMetadataUploadListener;
import org.commonjava.aprox.core.conf.DefaultProxyConfiguration;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.data.ProxyAppDescription;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.aprox.core.inject.AproxDataProviders;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.rest.RESTApplication;
import org.commonjava.aprox.sec.change.SecurityConsistencyListener;
import org.commonjava.aprox.sec.fixture.AProxSecTestPropertiesProvider;
import org.commonjava.aprox.sec.fixture.ProxyConfigProvider;
import org.commonjava.aprox.sec.rest.access.RepositoryAccessResourceSecurity;
import org.commonjava.aprox.sec.rest.admin.RepositoryAdminResourceSecurity;
import org.commonjava.aprox.sec.webctl.ShiroBasicAuthenticationFilter;
import org.commonjava.auth.couch.data.UserAppDescription;
import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.user.fixture.TestUserWarArchiveBuilder;
import org.commonjava.couch.user.web.test.AbstractUserRESTCouchTest;
import org.commonjava.web.test.fixture.TestData;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;

public class AbstractAProxSecLiveTest
    extends AbstractUserRESTCouchTest
{

    @Inject
    protected ProxyDataManager proxyManager;

    @Inject
    @AproxData
    protected CouchChangeListener changeListener;

    @Inject
    @AproxData
    private CouchManager couch;

    @Deployment
    public static WebArchive createWar()
    {
        TestUserWarArchiveBuilder builder =
            new TestUserWarArchiveBuilder( AProxSecTestPropertiesProvider.class );

        builder.withExtraClasses( AbstractAProxSecLiveTest.class, ProxyConfiguration.class,
                                  DefaultProxyConfiguration.class,
                                  TestUserManagerConfigProducer.class );

        builder.withExtraPackages( true,
                                   RESTApplication.class.getPackage(),
                                   RepositoryAccessResourceSecurity.class.getPackage(),
                                   RepositoryAdminResourceSecurity.class.getPackage(),
                                   ShiroBasicAuthenticationFilter.class.getPackage(),
                                   ProxyConfigProvider.class.getPackage(),
                                   Repository.class.getPackage(),
                                   ProxyDataManager.class.getPackage(),
                                   MavenMetadataUploadListener.class.getPackage(),
                                   SecurityConsistencyListener.class.getPackage(),
                                   Os.class.getPackage(), // grab all of plexus-utils
                                   Metadata.class.getPackage(),
                                   AproxDataProviders.class.getPackage(),
                                   AProxSecTestPropertiesProvider.class.getPackage() );

        builder.withStandardPackages();
        builder.withTestUserManagerConfigProducer();
        builder.withLog4jProperties();
        builder.withStandardAuthentication();
        builder.withAllStandards();
        builder.withApplication( new ProxyAppDescription() );
        builder.withApplication( new UserAppDescription() );

        WebArchive archive = builder.build();

        String path = "META-INF/beans.arq.xml";
        URL resource = Thread.currentThread().getContextClassLoader().getResource( path );
        if ( resource != null )
        {
            archive.addAsWebInfResource( resource, "classes/META-INF/beans.xml" );
        }
        else
        {
            throw new IllegalStateException(
                                             "Cannot find beans.xml ARQ resource from AS7 test harness!. Path: "
                                                 + path + ", Classloader: "
                                                 + TestData.class.getClassLoader() );
        }

        // String basedir = System.getProperty( "basedir", "." );
        // archive.addAsWebInfResource( new File( basedir, "src/test/resources/META-INF/beans.xml" ),
        // "classes/beans.xml" );

        return archive;
    }

    @Before
    public final void setupAProxLiveTest()
        throws Exception
    {
        proxyManager.install();
        changeListener.startup();
    }

    @After
    public final void teardownAProxLiveTest()
        throws Exception
    {
        changeListener.shutdown();
        while ( changeListener.isRunning() )
        {
            synchronized ( changeListener )
            {
                System.out.println( "Waiting 2s for change listener to shutdown..." );
                changeListener.wait( 2000 );
            }
        }

        couch.dropDatabase();
    }

    protected String getString( final String url, final int expectedStatus )
        throws ClientProtocolException, IOException
    {
        HttpResponse response = http.execute( new HttpGet( url ) );
        StatusLine sl = response.getStatusLine();

        assertThat( sl.getStatusCode(), equalTo( expectedStatus ) );
        assertThat( response.getEntity(), notNullValue() );

        StringWriter sw = new StringWriter();
        copy( response.getEntity().getContent(), sw );

        return sw.toString();
    }

    @Override
    protected CouchManager getCouchManager()
    {
        return couch;
    }

}
