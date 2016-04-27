/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.core.inject;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.maven.galley.config.TransportManagerConfig;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMaven304PluginDefaults;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMavenPluginImplications;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.model.FilePatternMatcher;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;

import java.util.Arrays;

@ApplicationScoped
public class GalleyProvider
{

    @Inject
    private XMLInfrastructure xml;

    private MavenPluginDefaults pluginDefaults;

    private MavenPluginImplications pluginImplications;

    private TransportManagerConfig transportManagerConfig;

    @Inject
    private SpecialPathManager specialPathManager;

    @PostConstruct
    public void setup()
    {
        pluginDefaults = new StandardMaven304PluginDefaults();
        pluginImplications = new StandardMavenPluginImplications( xml );

        // TODO: Tie this into a config file!
        transportManagerConfig = new TransportManagerConfig();

        // Register metadata file info for metadata timeout
        for ( String extPattern : Arrays.asList( ".*maven-metadata\\.xml$", ".*archetype-catalog\\.xml$" ) )
        {
            final SpecialPathInfo pi = SpecialPathInfo.from( new FilePatternMatcher( extPattern ) )
                                .setDecoratable( true )
                                .setListable( true )
                                .setPublishable( true )
                                .setRetrievable( true )
                                .setStorable( true )
                                .setMetadata( true )
                                .build();

            specialPathManager.registerSpecialPathInfo( pi );
        }
    }

    @Produces
    public TransportManagerConfig getTransportManagerConfig()
    {
        return transportManagerConfig;
    }

    @Produces
    public MavenPluginDefaults getPluginDefaults()
    {
        return pluginDefaults;
    }

    @Produces
    public MavenPluginImplications getPluginImplications()
    {
        return pluginImplications;
    }

}
