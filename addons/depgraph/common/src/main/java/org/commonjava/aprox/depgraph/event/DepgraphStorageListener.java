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
import org.commonjava.maven.cartographer.discover.post.patch.PatcherSupport;
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
