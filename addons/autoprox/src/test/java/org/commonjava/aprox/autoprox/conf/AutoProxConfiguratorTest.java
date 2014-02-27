package org.commonjava.aprox.autoprox.conf;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.subsys.template.ScriptEngine;
import org.commonjava.web.config.ConfigurationException;
import org.junit.Test;

public class AutoProxConfiguratorTest
{

    private final AutoProxConfigurator configurator = new AutoProxConfigurator( new ScriptEngine() );

    @Test
    public void createFactoryFromGroovyWithRegexNameSplitting()
        throws ConfigurationException, MalformedURLException
    {
        configurator.parameter( AutoProxConfigurator.ENABLED_PARAM, "true" );

        final File script = getScript( "autoprox.d/factory-with-regex.groovy" );
        configurator.parameter( AutoProxConfigurator.BASEDIR_PARAM, script.getParent() );
        configurator.parameter( "prod-.+\\d.+", script.getName() );

        final AutoProxConfig config = configurator.getConfig();
        final List<FactoryMapping> mappings = config.getFactoryMappings();

        assertThat( mappings.size(), equalTo( 1 ) );

        final AutoProxFactory factory = mappings.get( 0 )
                                                .getFactory();

        final RemoteRepository remote = factory.createRemoteRepository( "prod-foo1.2.3" );
        final String url = remote.getUrl();

        assertThat( url, equalTo( "http://repository.myco.com/products/foo1/1.2.3/" ) );
    }

    public File getScript( final String resource )
    {
        final URL url = Thread.currentThread()
                              .getContextClassLoader()
                              .getResource( resource );
        return new File( url.getPath() );
    }

}
