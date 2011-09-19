package org.commonjava.web.maven.proxy;

import static org.apache.commons.io.IOUtils.copy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.StringWriter;

import javax.inject.Inject;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.cjtest.fixture.TestUserManagerConfigProducer;
import org.commonjava.auth.couch.data.UserAppDescription;
import org.commonjava.couch.change.CouchChangeListener;
import org.commonjava.web.maven.proxy.change.RepositoryDeletionListener;
import org.commonjava.web.maven.proxy.conf.DefaultProxyConfiguration;
import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;
import org.commonjava.web.maven.proxy.data.ProxyAppDescription;
import org.commonjava.web.maven.proxy.data.ProxyDataManager;
import org.commonjava.web.maven.proxy.fixture.AProxTestPropertiesProvider;
import org.commonjava.web.maven.proxy.fixture.ProxyConfigProvider;
import org.commonjava.web.maven.proxy.model.Repository;
import org.commonjava.web.maven.proxy.rest.RESTApplication;
import org.commonjava.web.test.AbstractRESTCouchTest;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;

public class AbstractAProxLiveTest
    extends AbstractRESTCouchTest
{

    @Inject
    protected ProxyDataManager proxyManager;

    @Inject
    protected CouchChangeListener changeListener;

    @Deployment
    public static WebArchive createWar()
    {
        TestWarArchiveBuilder builder =
            new TestWarArchiveBuilder( AProxTestPropertiesProvider.class );

        builder.withExtraClasses( AbstractAProxLiveTest.class, AProxTestPropertiesProvider.class,
                                  ProxyConfigProvider.class, ProxyConfiguration.class,
                                  TestUserManagerConfigProducer.class,
                                  DefaultProxyConfiguration.class );

        builder.withExtraPackages( true, RESTApplication.class.getPackage(),
                                   Repository.class.getPackage(),
                                   ProxyDataManager.class.getPackage(),
                                   RepositoryDeletionListener.class.getPackage() );

        builder.withAllStandards();
        builder.withApplication( new ProxyAppDescription() );
        builder.withApplication( new UserAppDescription() );

        return builder.build();
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

}
