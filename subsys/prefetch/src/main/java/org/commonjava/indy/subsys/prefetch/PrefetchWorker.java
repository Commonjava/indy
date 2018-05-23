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

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.ListingResult;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.util.PathUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrefetchWorker
        implements Runnable
{
    private TransferManager transfers;

    private PrefetchManager prefetchManager;

    private PrefetchFrontier frontier;

    private Map<RemoteRepository, List<ConcreteResource>> resources;

    private final Logger logger;

    private static final String LISTING_HTML_FILE = "index.html";

    public PrefetchWorker( final TransferManager transfers, final PrefetchFrontier frontier,
                           Map<RemoteRepository, List<ConcreteResource>> resources,
                           final PrefetchManager prefetchManager, final Logger logger )
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
            logger.info( "No resources for downloading" );
            return;
        }

        logger.info( "Start downloading: {}", resources );
        final AtomicBoolean scheduled = new AtomicBoolean( false );
        for ( Map.Entry<RemoteRepository, List<ConcreteResource>> entry : resources.entrySet() )
        {
            final RemoteRepository repo = entry.getKey();
            final List<ConcreteResource> res = entry.getValue();
            res.forEach( r -> {
                try
                {
                    final String path = r.getPath();
                    if ( path == null || path.equals( "" ) || path.endsWith( "/" ) || path.endsWith(
                            LISTING_HTML_FILE ) )
                    {
                        List<String> dirPaths = buildPaths( r );
                        logger.info( "{} is folder, will use it to schedule new Resources {}", r, dirPaths );
                        frontier.scheduleRepo( repo, dirPaths );
                        scheduled.set( true );
                    }
                    else
                    {
                        logger.info( "{} is file", r );
                        final Transfer tr = transfers.retrieve( r );
                        if ( !exists( tr ) )
                        {
                            logger.warn( "Download failed during prefetch for {}, reason is resource not exists", r );
                        }

                    }
                }
                catch ( TransferException e )
                {
                    logger.error(
                            "Download failed during prefetch because of transfer getting failed for {}, Reason: {}", r,
                            e.getMessage() );
                }
            } );
        }

        if ( scheduled.get() )
        {
            prefetchManager.triggerWorkers();
        }
    }

    private boolean exists( final Transfer transfer )
    {
        return transfer != null && transfer.exists();
    }

    private List<String> buildPaths( final ConcreteResource resource )
    {
        try
        {
            ListingResult lr = transfers.list( resource );
            if ( lr != null && lr.getListing() != null )
            {
                String[] files = lr.getListing();
                List<String> paths = new ArrayList<>( files.length );
                for ( final String file : files )
                {
                    paths.add( PathUtils.normalize( resource.getPath(), file ) );
                }
                return paths;
            }
            else
            {
                logger.info( "No content found for {}", resource );
            }
        }
        catch ( TransferException e )
        {
            logger.error( "List content failed during prefetch because of transfer getting failed for {}, Reason: {}",
                          resource, e.getMessage() );
        }
        return Collections.emptyList();
    }

}
