/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.autoprox.conf;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.commonjava.aprox.autoprox.data.AutoProxRule;
import org.commonjava.aprox.autoprox.data.RuleMapping;
import org.commonjava.aprox.autoprox.util.ScriptRuleParser;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.subsys.template.ScriptEngine;
import org.commonjava.web.config.ConfigurationException;
import org.junit.Test;

public class AutoProxConfiguratorTest
{

    private final AutoProxConfigurator configurator =
        new AutoProxConfigurator( new ScriptRuleParser( new ScriptEngine() ) );

    @Test
    @Deprecated
    public void createFactoryFromGroovyWithRegexNameSplitting()
        throws ConfigurationException, MalformedURLException
    {
        configurator.parameter( AutoProxConfigurator.ENABLED_PARAM, "true" );

        final File script = getScript( "autoprox.d/factory-with-regex.groovy" );
        configurator.parameter( AutoProxConfigurator.BASEDIR_PARAM, script.getParent() );
        configurator.parameter( "prod-.+\\d.+", script.getName() );

        final AutoProxConfig config = configurator.getConfig();
        final List<RuleMapping> mappings = config.getRuleMappings();

        assertThat( mappings.size(), equalTo( 1 ) );

        final AutoProxRule factory = mappings.get( 0 )
                                             .getRule();

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
