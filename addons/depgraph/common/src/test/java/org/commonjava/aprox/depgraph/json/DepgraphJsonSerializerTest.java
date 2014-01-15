/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
