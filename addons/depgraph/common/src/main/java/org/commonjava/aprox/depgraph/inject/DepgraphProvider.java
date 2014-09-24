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
package org.commonjava.aprox.depgraph.inject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;

@ApplicationScoped
public class DepgraphProvider
{

    @Inject
    private AproxDepgraphConfig config;

    private RelationshipGraphFactory graphFactory;

    protected DepgraphProvider()
    {
    }

    public DepgraphProvider( final AproxDepgraphConfig config )
    {
        this.config = config;
        init();
    }

    @PostConstruct
    public void init()
    {
        this.graphFactory =
            new RelationshipGraphFactory( new FileNeo4jConnectionFactory( config.getDataBasedir(), true ) );
    }

    @PreDestroy
    public void shutdown()
        throws RelationshipGraphException
    {
        this.graphFactory.close();
    }

    @Produces
    public RelationshipGraphFactory getGraphFactory()
    {
        return graphFactory;
    }

}
