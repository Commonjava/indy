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
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.json.DepgraphSerializationAdapter;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.maven.atlas.graph.RelationshipGraphException;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.maven.atlas.graph.spi.neo4j.FileNeo4jConnectionFactory;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class DepgraphProvider
{

    @Inject
    private AproxDepgraphConfig config;

    private RelationshipGraphFactory graphFactory;

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

    @Produces
    @DepgraphSpecific
    @Default
    public synchronized JsonSerializer getJsonSerializer()
    {
        if ( serializer == null )
        {
            serializer =
                new JsonSerializer( new StoreKeySerializer(), new DepgraphSerializationAdapter(),
                                    new PrettyPrintAdapter() );
        }

        return serializer;
    }

}
