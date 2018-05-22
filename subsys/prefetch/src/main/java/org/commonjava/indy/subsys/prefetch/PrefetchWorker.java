/**
 * Copyright (C) 2013 Red Hat, Inc.
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
package org.commonjava.indy.subsys.prefetch;

import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.ListingResult;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrefetchWorker
        implements Runnable
{
    private TransferManager transfers;

    private PrefetchManager prefetchManager;

    private PrefetchFrontier frontier;

    private List<PrioritizedResource> resources;

    private final Logger logger;

    private static final String LISTING_HTML_FILE = "index.html";

    public PrefetchWorker( final TransferManager transfers, final PrefetchFrontier frontier,
                           List<PrioritizedResource> resources, final PrefetchManager prefetchManager,
                           final Logger logger )
    {
        this.transfers = transfers;
        this.frontier = frontier;
        this.resources = resources;
        this.prefetchManager = prefetchManager;
        this.logger = logger;
    }

    @Override
    public void run()
    {
        if ( resources == null || resources.isEmpty() )
        {
            logger.trace( "No resources for downloading" );
            return;
        }

        logger.trace( "Start downloading: {}", resources );
        resources.forEach( r -> {
            try
            {
                final String path = r.getResource().getPath();
                if ( path == null || path.equals( "" ) || path.endsWith( "/" ) || path.endsWith( LISTING_HTML_FILE ) )
                {
                    List<PrioritizedResource> dirResources = buildContent( r );
                    logger.trace( "{} is dir, will use it to schedule new Resources {}", r, dirResources );
                    frontier.scheduleResources( dirResources );
                    prefetchManager.triggerWorkers();
                }
                else
                {
                    logger.trace( "{} is not dir", r );
                    final Transfer tr = transfers.retrieve( r.getResource() );
                    if ( !exists( tr ) )
                    {
                        logger.warn( "Download failed during prefetch for {}, reason is resource not exists",
                                     r.getResource() );
                    }

                }
            }
            catch ( TransferException e )
            {
                logger.error( "Download failed during prefetch because of transfer getting failed for {}, Reason: {}",
                              r.getResource(), e.getMessage() );
            }
        } );
    }

    private boolean exists( final Transfer transfer )
    {
        return transfer != null && transfer.exists();
    }

    private List<PrioritizedResource> buildContent( final PrioritizedResource resource )
    {
        try
        {
            final ConcreteResource r = resource.getResource();
            ListingResult lr = transfers.list( resource.getResource() );
            if ( lr != null && lr.getListing() != null )
            {
                String[] files = lr.getListing();
                List<PrioritizedResource> resources = new ArrayList<>( files.length );
                for ( final String file : files )
                {
                    resources.add( new PrioritizedResource(
                            new StoreResource( (KeyedLocation) lr.getLocation(), r.getPath(), file ),
                            resource.getPriority() ) );
                }
                return resources;
            }
            else
            {
                logger.trace( "No content found for {}", resource.getResource() );
            }
        }
        catch ( TransferException e )
        {
            logger.error( "List content failed during prefetch because of transfer getting failed for {}, Reason: {}",
                          resource.getResource(), e.getMessage() );
        }
        return Collections.emptyList();
    }

}
