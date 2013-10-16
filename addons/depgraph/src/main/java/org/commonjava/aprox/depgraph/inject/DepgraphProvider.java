package org.commonjava.aprox.depgraph.inject;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.json.DepgraphSerializationAdapter;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jWorkspaceFactory;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.galley.maven.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.maven.defaults.StandardMaven304PluginDefaults;
import org.commonjava.maven.galley.maven.defaults.StandardMavenPluginImplications;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class DepgraphProvider
{

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private CartoDataManager data;

    @Inject
    private XMLInfrastructure xml;

    private MavenPluginDefaults pluginDefaults;

    private MavenPluginImplications pluginImplications;

    private EGraphManager graphs;

    private JsonSerializer serializer;

    protected DepgraphProvider()
    {
    }

    public DepgraphProvider( final AproxDepgraphConfig config )
    {
        this.config = config;
        setup();
    }

    @PostConstruct
    public void setup()
    {
        this.graphs = new EGraphManager( new FileNeo4jWorkspaceFactory( config.getDatabaseDir(), false ) );
        pluginDefaults = new StandardMaven304PluginDefaults();
        pluginImplications = new StandardMavenPluginImplications( xml );
    }

    @PreDestroy
    public void shutdown()
        throws IOException
    {
        this.graphs.close();
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

    @Produces
    public EGraphManager getGraphs()
    {
        return graphs;
    }

    @Produces
    @DepgraphSpecific
    @Default
    public synchronized JsonSerializer getJsonSerializer()
    {
        if ( serializer == null )
        {
            serializer = new JsonSerializer( new StoreKeySerializer(), new DepgraphSerializationAdapter( data ), new PrettyPrintAdapter() );
        }

        return serializer;
    }

}
