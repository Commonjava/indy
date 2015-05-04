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
package org.commonjava.aprox.depgraph.ftest.testutil;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.jaxrs.util.JaxRsPresetParamParser;
import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.ArtifactMetadataManager;
import org.commonjava.maven.galley.maven.internal.ArtifactManagerImpl;
import org.commonjava.maven.galley.maven.internal.ArtifactMetadataManagerImpl;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMaven304PluginDefaults;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMavenPluginImplications;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.internal.version.VersionResolverImpl;
import org.commonjava.maven.galley.maven.model.view.XPathManager;
import org.commonjava.maven.galley.maven.parse.MavenMetadataReader;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.maven.spi.version.VersionResolver;
import org.commonjava.maven.galley.spi.transport.LocationExpander;

@ApplicationScoped
public class TestProvider
{

    private PresetParameterParser presetParser;

    private MavenPomReader pomReader;

    private MavenMetadataReader metadataReader;

    private ArtifactManager artifacts;

    private ArtifactMetadataManager metadataManager;

    private MavenPluginImplications pluginImplications;

    private MavenPluginDefaults pluginDefaults;

    private XPathManager xpath;

    @Inject
    private LocationExpander locations;

    private XMLInfrastructure xml;

    private VersionResolver versionResolver;

    private TypeMapper mapper;

    @Inject
    private TransferManager transfers;

    @PostConstruct
    public void init()
    {
        xml = new XMLInfrastructure();
        xpath = new XPathManager();
        presetParser = new JaxRsPresetParamParser();

        pluginImplications = new StandardMavenPluginImplications( xml );
        pluginDefaults = new StandardMaven304PluginDefaults();

        mapper = new StandardTypeMapper();
        //        transfers = new TransferManagerImpl( transports, cache, nfc, fileEvents, downloader, uploader, lister, exister, executor );

        metadataManager = new ArtifactMetadataManagerImpl( transfers, locations );
        metadataReader = new MavenMetadataReader( xml, locations, metadataManager, xpath );
        versionResolver = new VersionResolverImpl( metadataReader );
        artifacts = new ArtifactManagerImpl( transfers, locations, mapper, versionResolver );
        pomReader = new MavenPomReader( xml, locations, artifacts, xpath, pluginDefaults, pluginImplications );
    }

    @Produces
    @Default
    @TestData
    public PresetParameterParser getPresetParameterParser()
    {
        return presetParser;
    }

    @Produces
    @Default
    @TestData
    public ArtifactManager getArtifactManager()
    {
        return artifacts;
    }

    @Produces
    @Default
    @TestData
    public MavenPomReader getMavenPomReader()
    {
        return pomReader;
    }

}
