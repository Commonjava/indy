package org.commonjava.aprox.tensor.fixture;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.discover.AproxDiscoverySourceManager;
import org.commonjava.aprox.depgraph.discover.AproxProjectGraphDiscoverer;
import org.commonjava.aprox.depgraph.event.AproxDepgraphEvents;
import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4JEGraphDriver;
import org.commonjava.maven.cartographer.agg.DefaultGraphAggregator;
import org.commonjava.maven.cartographer.agg.GraphAggregator;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.data.DefaultCartoDataManager;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.cartographer.discover.ProjectRelationshipDiscoverer;
import org.commonjava.maven.cartographer.discover.DiscovererImpl;
import org.commonjava.maven.cartographer.ops.CalculationOps;
import org.commonjava.maven.cartographer.ops.GraphOps;
import org.commonjava.maven.cartographer.ops.GraphRenderingOps;
import org.commonjava.maven.cartographer.ops.MetadataOps;
import org.commonjava.maven.cartographer.ops.ResolveOps;
import org.commonjava.maven.cartographer.ops.WorkspaceOps;
import org.commonjava.maven.cartographer.util.MavenModelProcessor;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class TestDepgraphProvider
{

    @Inject
    private AproxDepgraphEvents events;

    @Inject
    private StoreDataManager stores;

    @Inject
    private DiscoverySourceManager sourceFactory;

    @Inject
    private TransferManager xferMgr;

    private File dbDir;

    private TransferManager transferManager;

    private AproxDepgraphConfig config;

    private AproxDiscoverySourceManager sourceManager;

    private AproxProjectGraphDiscoverer discoverer;

    private ExecutorService aggregatorExecutor;

    private CartoDataManager data;

    private WorkspaceOps workspaces;

    private CalculationOps calculator;

    private GraphOps grapher;

    private GraphRenderingOps renderer;

    private MetadataOps metadata;

    private ResolveOps resolver;

    @PostConstruct
    public void setup()
    {
        createDirs();

        final EGraphManager graphs = new EGraphManager( new FileNeo4JEGraphDriver( dbDir ) );

        final GraphWorkspaceHolder wsHolder = new GraphWorkspaceHolder();

        final CartoDataManager data = new DefaultCartoDataManager( graphs, wsHolder, events );
        this.workspaces = new WorkspaceOps( data, sourceFactory );

        this.calculator = new CalculationOps( data );
        this.grapher = new GraphOps( data );
        this.renderer = new GraphRenderingOps( data );
        this.metadata = new MetadataOps( data );

        final MavenModelProcessor mmp = new MavenModelProcessor( data );

        final ProjectRelationshipDiscoverer discoverer = new DiscovererImpl( data, mmp, xferMgr );
        final GraphAggregator aggregator =
            new DefaultGraphAggregator( data, discoverer, Executors.newFixedThreadPool( 2 ) );
        this.resolver = new ResolveOps( data, sourceFactory, discoverer, aggregator );

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

    private void createDirs()
        throws IOException
    {
        dbDir = File.createTempFile( "tensor.", ".db" );
        dbDir.delete();
        dbDir.mkdirs();
    }

    @PreDestroy
    public void shutdown()
    {
        if ( dbDir.exists() )
        {
            try
            {
                FileUtils.forceDelete( dbDir );
            }
            catch ( final IOException e )
            {
                new Logger( getClass() ).error( e.getMessage(), e );
            }
        }
    }

}
