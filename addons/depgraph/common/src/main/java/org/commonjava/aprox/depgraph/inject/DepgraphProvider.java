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
