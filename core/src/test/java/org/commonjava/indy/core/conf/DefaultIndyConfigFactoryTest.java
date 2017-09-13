/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.core.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.galley.config.TransportManagerConfig;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.PluginDependencyView;
import org.commonjava.maven.galley.maven.model.view.PluginView;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginImplications;
import org.commonjava.web.config.ConfigUtils;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.section.ConfigurationSectionListener;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.enterprise.inject.Produces;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 3/9/16.
 */
public class DefaultIndyConfigFactoryTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void weldInjection_IterateIndyConfigurators()
    {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();

        List<String> sections = new ArrayList<>();
        container.instance().select( IndyConfigInfo.class ).iterator().forEachRemaining( ( instance)->{
            String section = ConfigUtils.getSectionName( instance );
            System.out.printf( "Got instance: %s with section: %s\n", instance, section );
            sections.add( section );
        } );

        System.out.println(sections);
        assertThat( sections.contains( ConfigurationSectionListener.DEFAULT_SECTION ), equalTo( true ) );
        assertThat( sections.contains( "flatfiles" ), equalTo( true ) );
        assertThat( sections.contains( "ui" ), equalTo( true ) );
        assertThat( sections.contains( "storage-default" ), equalTo( true ) );
    }

    @Test
    public void weldInjection_writeDefaults()
            throws IOException, ConfigurationException
    {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();

        DefaultIndyConfigFactory factory = container.instance().select( DefaultIndyConfigFactory.class ).get();

        File dir = temp.newFolder( "indy-config" );
        dir.mkdirs();

        factory.writeDefaultConfigs( dir );

        assertThat( new File( dir, "main.conf" ).exists(), equalTo( true ) );
    }

}
