package org.commonjava.aprox.autoprox.live.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.MalformedURLException;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.autoprox.conf.AutoProxConfiguration;
import org.commonjava.aprox.autoprox.conf.DefaultAutoProxConfiguration;
import org.commonjava.aprox.autoprox.live.fixture.TargetUrlResponder;
import org.commonjava.aprox.core.conf.DefaultProxyConfiguration;
import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.ModelFactory;
import org.commonjava.aprox.core.model.Repository;
import org.commonjava.web.test.fixture.TestWarArchiveBuilder;
import org.commonjava.web.test.fixture.WebFixture;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class AutoProxDataManagerDecoratorTest
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    @Inject
    protected ProxyDataManager proxyManager;

    @Inject
    protected AutoProxConfiguration config;

    @Inject
    protected ModelFactory modelFactory;

    @Inject
    protected TargetUrlResponder targetResponder;

    @Rule
    public final WebFixture http = new WebFixture();

    @Before
    public final void setup()
        throws Exception
    {
        proxyManager.install();
    }

    @After
    public final void teardown()
    {
        targetResponder.clearTargets();
    }

    @Deployment
    public static WebArchive createWar()
    {
        return new TestWarArchiveBuilder( AutoProxDataManagerDecoratorTest.class ).withExtraClasses( TargetUrlResponder.class )
                                                                                  .withLibrariesIn( new File(
                                                                                                              "target/dependency" ) )
                                                                                  .withLog4jProperties()
                                                                                  .withBeansXml( "beans.xml.autoprox" )
                                                                                  .build();
    }

    @Singleton
    public static final class ConfigProvider
    {
        private AutoProxConfiguration autoProxConfig;

        private ProxyConfiguration proxyConfig;

        @Produces
        @Default
        public synchronized ProxyConfiguration getProxyConfig()
        {
            if ( proxyConfig == null )
            {
                proxyConfig =
                    new DefaultProxyConfiguration(
                                                   new File(
                                                             System.getProperty( REPO_ROOT_DIR, "target/repo-downloads" ) ) );
            }
            return proxyConfig;
        }

        @Produces
        @Default
        public synchronized AutoProxConfiguration getAutoProxConfig()
            throws MalformedURLException
        {
            if ( autoProxConfig == null )
            {
                autoProxConfig = new DefaultAutoProxConfiguration( "http://localhost:8080/test/api/1.0/target" );
            }

            return autoProxConfig;
        }
    }

    @Test
    public void repositoryAutoCreated()
        throws Exception
    {
        targetResponder.approveTargets( "target" );

        config.setEnabled( false );
        assertThat( proxyManager.getRepository( "test" ), nullValue() );
        config.setEnabled( true );

        final Repository repo = proxyManager.getRepository( "test" );

        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "test" ) );
        assertThat( repo.getUrl(), equalTo( http.resourceUrl( "target", "test" ) ) );
    }

}
