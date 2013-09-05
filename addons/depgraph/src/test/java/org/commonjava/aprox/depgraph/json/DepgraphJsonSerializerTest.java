package org.commonjava.aprox.depgraph.json;

import org.apache.log4j.Level;
import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jWorkspaceFactory;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.cartographer.data.DefaultCartoDataManager;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.maven.cartographer.event.NoOpCartoEventManager;
import org.commonjava.util.logging.Log4jUtil;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DepgraphJsonSerializerTest
{

    private DefaultCartoDataManager data;

    private JsonSerializer serializer;

    private EGraphManager graphs;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final Logger logger = new Logger( getClass() );

    @BeforeClass
    public static void beforeClass()
    {
        Log4jUtil.configure( Level.DEBUG );
    }

    @Before
    public void before()
    {
        graphs = new EGraphManager( new FileNeo4jWorkspaceFactory( temp.newFolder( "db" ), false ) );
        data = new DefaultCartoDataManager( graphs, new GraphWorkspaceHolder(), new NoOpCartoEventManager() );

        serializer = new JsonSerializer( new DepgraphSerializationAdapter( data ) );
    }

    @After
    public void after()
        throws Exception
    {
        graphs.close();
    }

    @Test
    public void serializeWorkspace()
        throws Exception
    {
        final GraphWorkspace ws = data.createWorkspace( new GraphWorkspaceConfiguration() );
        final String json = serializer.toString( ws );

        logger.info( "Serialized workspace:\n\n  %s\n\n", json );
    }

}
