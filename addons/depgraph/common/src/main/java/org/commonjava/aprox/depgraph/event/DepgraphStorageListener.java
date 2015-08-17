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
package org.commonjava.aprox.depgraph.event;

import java.util.concurrent.ExecutorService;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.depgraph.DepgraphStorageListenerRunnable;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.depgraph.discover.AproxModelDiscoverer;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.atlas.graph.RelationshipGraphFactory;
import org.commonjava.cartographer.graph.discover.patch.PatcherSupport;
import org.commonjava.maven.galley.event.FileAccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class DepgraphStorageListener
{

    @Inject
    private AproxModelDiscoverer discoverer;

    @Inject
    private StoreDataManager aprox;

    @Inject
    @ExecutorConfig( priority = 8, threads = 2, named = "depgraph-listener" )
    private ExecutorService executor;

    @Inject
    private PatcherSupport patcherSupport;

    @Inject
    private RelationshipGraphFactory graphFactory;

    @Inject
    private AproxDepgraphConfig config;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public void handleFileAccessEvent( @Observes final FileAccessEvent event )
    {
        if ( !config.isPassiveParsingEnabled() )
        {
            return;
        }

        if ( !event.getTransfer()
                   .getPath()
                   .endsWith( ".pom" ) )
        {
            return;
        }

        logger.info( "[SUBMIT] DepgraphStorageListenerRunnable for: {}", event );

        executor.execute( new DepgraphStorageListenerRunnable( discoverer, aprox, graphFactory, patcherSupport,
                                                               event.getTransfer() ) );
    }
}
