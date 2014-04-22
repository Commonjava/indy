/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.depgraph.json;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jWorkspaceFactory;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspace;
import org.commonjava.maven.atlas.graph.workspace.GraphWorkspaceConfiguration;
import org.commonjava.maven.cartographer.data.DefaultCartoDataManager;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.maven.cartographer.event.NoOpCartoEventManager;
import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DepgraphJsonSerializerTest
{

    private DefaultCartoDataManager data;

    private JsonSerializer serializer;

    private EGraphManager graphs;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Before
    public void before()
    {
        graphs = new EGraphManager( new FileNeo4jWorkspaceFactory( temp.newFolder( "db" ), false ) );
        data = new DefaultCartoDataManager( graphs, new GraphWorkspaceHolder(), new NoOpCartoEventManager() );

        serializer = new JsonSerializer( new DepgraphSerializationAdapter() );
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

        logger.info( "Serialized workspace:\n\n  {}\n\n", json );
    }

}
