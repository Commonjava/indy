package org.commonjava.aprox.depgraph.ftest.testutil;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.aprox.depgraph.vertx.util.VertXPresetParamParser;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.internal.ArtifactManagerImpl;
import org.commonjava.maven.galley.maven.model.view.XPathManager;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.maven.spi.version.VersionResolver;
import org.commonjava.maven.galley.spi.transport.LocationExpander;

import com.google.inject.Inject;

@ApplicationScoped
public class TestProvider
{

    private PresetParameterParser presetParser;

    private MavenPomReader pomReader;

    private ArtifactManager artifacts;

    @Inject
    private MavenPluginImplications pluginImplications;

    @Inject
    private MavenPluginDefaults pluginDefaults;

    @Inject
    private XPathManager xpath;

    @Inject
    private LocationExpander locations;

    @Inject
    private XMLInfrastructure xml;

    @Inject
    private VersionResolver versionResolver;

    @Inject
    private TypeMapper mapper;

    @Inject
    private TransferManager transfers;

    @PostConstruct
    public void init()
    {
        presetParser = new VertXPresetParamParser();
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
