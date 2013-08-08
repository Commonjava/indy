package org.commonjava.aprox.depgraph.inject;

import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.discover.AproxDiscoverySourceManager;
import org.commonjava.aprox.depgraph.discover.AproxProjectGraphDiscoverer;
import org.commonjava.aprox.depgraph.event.AproxDepgraphEvents;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4JEGraphDriver;
import org.commonjava.maven.cartographer.agg.DefaultGraphAggregator;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.data.DefaultCartoDataManager;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.maven.cartographer.ops.CalculationOps;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.ops.WorkspaceOps;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.TransferManager;

@ApplicationScoped
public class DepgraphProvider
{

    @Inject
    private TransferManager transferManager;

    @Inject
    private AproxDepgraphConfig config;

    @Inject
    private AproxDepgraphEvents events;

    @Inject
    private AproxDiscoverySourceManager sourceManager;

    @Inject
    private AproxProjectGraphDiscoverer discoverer;

    @Inject
    @ExecutorConfig( daemon = true, named = "graph-aggregator", priority = 9, threads = 2 )
    private ExecutorService aggregatorExecutor;

    private MavenModelProcessor modelProcessor;

    private CartoDataManager data;

    private WorkspaceOps workspaces;

    private CalculationOps calculator;

    private GraphOps grapher;

    private GraphRenderingOps renderer;

    private MetadataOps metadata;

    private ResolveOps resolver;

    protected DepgraphProvider()
    {
    }

    public DepgraphProvider( final TransferManager transferManager, final AproxDepgraphConfig config,
                             final AproxDepgraphEvents events, final AproxDiscoverySourceManager sourceManager,
                             final AproxProjectGraphDiscoverer discoverer, final ExecutorService aggregatorExecutor )
    {
        this.transferManager = transferManager;
        this.config = config;
        this.events = events;
        this.sourceManager = sourceManager;
        this.discoverer = discoverer;
        this.aggregatorExecutor = aggregatorExecutor;
        setup();
    }

    @PostConstruct
    public void setup()
    {
        final EGraphManager graphs = new EGraphManager( new FileNeo4JEGraphDriver( config.getDatabaseDir() ) );

        final GraphWorkspaceHolder wsHolder = new GraphWorkspaceHolder();

        this.data = new DefaultCartoDataManager( graphs, wsHolder, events );

        this.workspaces = new WorkspaceOps( data, sourceManager );

        this.calculator = new CalculationOps( data );
        this.grapher = new GraphOps( data );
        this.renderer = new GraphRenderingOps( data );
        this.metadata = new MetadataOps( data );

        final GraphAggregator aggregator = new DefaultGraphAggregator( data, discoverer, aggregatorExecutor );
        this.resolver = new ResolveOps( data, sourceManager, discoverer, aggregator );
    }

    @Produces
    public WorkspaceOps getWorkspaceOps()
    {
        return workspaces;
    }

    @Produces
    public CalculationOps getCalculationOps()
    {
        return calculator;
    }

    @Produces
    public GraphOps getGraphOps()
    {
        return grapher;
    }

    @Produces
    public GraphRenderingOps getRenderingOps()
    {
        return renderer;
    }

    @Produces
    public MetadataOps getMetadataOps()
    {
        return metadata;
    }

    @Produces
    public ResolveOps getResolveOps()
    {
        return resolver;
    }

    @Produces
    public CartoDataManager getCartoDataManager()
    {
        return data;
    }

}
