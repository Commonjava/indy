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
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4JEGraphDriver;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class DepgraphProvider
{

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private GraphWorkspaceHolder workspaceHolder;

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
        this.graphs = new EGraphManager( new FileNeo4JEGraphDriver( config.getDatabaseDir() ) );
        this.serializer =
            new JsonSerializer( new StoreKeySerializer(), new DepgraphSerializationAdapter( graphs, workspaceHolder ) );
    }

    @PreDestroy
    public void shutdown()
        throws IOException
    {
        this.graphs.close();
    }

    @Produces
    public EGraphManager getGraphs()
    {
        return graphs;
    }

    @Produces
    @DepgraphSpecific
    @Default
    public JsonSerializer getJsonSerializer()
    {
        return serializer;
    }

}
