/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.core.content.PathMaskChecker;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.subsys.prefetch.models.RescanablePath;
import org.commonjava.indy.subsys.prefetch.models.RescanableResourceWrapper;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.ListingResult;
import org.commonjava.maven.galley.model.SpecialPathInfo;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.commonjava.maven.galley.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrefetchWorker
        implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger( PrefetchWorker.class );

    private TransferManager transfers;

    private PrefetchManager prefetchManager;

    private PrefetchFrontier frontier;

    private Map<RemoteRepository, List<RescanableResourceWrapper>> resources;

    private SpecialPathManager specialPathManager;


    private static final String LISTING_HTML_FILE = "index.html";

    public PrefetchWorker( final TransferManager transfers, final PrefetchFrontier frontier,
                           Map<RemoteRepository, List<RescanableResourceWrapper>> resources,
                           final PrefetchManager prefetchManager, final SpecialPathManager specialPathManager)
    {
        this.transfers = transfers;
        this.frontier = frontier;
        this.resources = resources;
        this.prefetchManager = prefetchManager;
        this.specialPathManager = specialPathManager;
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
        final AtomicBoolean scheduled = new AtomicBoolean( false );
        for ( Map.Entry<RemoteRepository, List<RescanableResourceWrapper>> entry : resources.entrySet() )
        {
            final RemoteRepository repo = entry.getKey();
            final List<RescanableResourceWrapper> res = entry.getValue();
            res.forEach( r -> {
                try
                {
                    final String path = r.getResource().getPath();
                    if ( path == null || path.equals( "" ) || path.endsWith( "/" ) || path.endsWith(
                            LISTING_HTML_FILE ) )
                    {
                        // If this is a rescan prefetch, we need to clear the listing cache and re-fetch from external
                        if ( r.isRescan() )
                        {
                            transfers.delete(
                                    new ConcreteResource( r.getResource().getLocation(), path, ".listing.txt" ) );
                        }
                        final List<RescanablePath> dirPaths = buildPaths( r.getResource(), r.isRescan() );
                        logger.trace( "{} is folder, will use it to schedule new Resources {}", r, dirPaths );
                        frontier.scheduleRepo( repo, dirPaths );
                        scheduled.set( true );
                    }
                    else
                    {
                        // if repo has path masks, we need to check that first to only download path mask enabled artifacts.
                        if ( PathMaskChecker.checkMask( repo, path ) )
                        {
                            // If this is a rescan prefetch, and artifact is metadata, we need to clear it and re-fetch from external
                            if ( r.isRescan() )
                            {
                                final SpecialPathInfo spi = specialPathManager.getSpecialPathInfo( r.getResource() );
                                if ( spi != null && spi.isMetadata() )
                                {
                                    transfers.delete( r.getResource() );
                                }
                            }
                            logger.trace( "{} is file", r );
                            transfers.retrieve( r.getResource() );
                        }
                        else
                        {
                            logger.trace( "Path {} in repo {} not available for path mask {}", path, repo,
                                          repo.getPathMaskPatterns() );
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

    private List<RescanablePath> buildPaths( final ConcreteResource resource, final boolean isRescan )
    {
        try
        {
            ListingResult lr = transfers.list( resource );
            if ( lr != null && lr.getListing() != null )
            {
                String[] files = lr.getListing();
                List<RescanablePath> paths = new ArrayList<>( files.length );
                for ( final String file : files )
                {
                    paths.add( new RescanablePath( PathUtils.normalize( resource.getPath(), file ), isRescan ) );
                }
                return paths;
            }
            else
            {
                logger.trace( "No content found for {}", resource );
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
