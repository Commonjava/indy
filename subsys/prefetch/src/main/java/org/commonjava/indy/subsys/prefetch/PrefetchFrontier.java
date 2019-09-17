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

import org.apache.commons.lang.StringUtils;
import org.commonjava.cdi.util.weft.Locker;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.subsys.prefetch.conf.PrefetchConfig;
import org.commonjava.indy.subsys.prefetch.models.RescanablePath;
import org.commonjava.indy.subsys.prefetch.models.RescanableResourceWrapper;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.commonjava.indy.subsys.prefetch.RescanTimeUtils.*;

@ApplicationScoped
public class PrefetchFrontier
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    @PrefetchCache
    private CacheHandle<RemoteRepository, List> resourceCache;

    @Inject
    private PrefetchConfig config;

    @Inject
    private StoreDataManager storeDataManager;

    private final List<RemoteRepository> repoQueue = new ArrayList<>();

    private volatile boolean shouldSchedule = true;

    // Use this volatile to avoid lock on hasMore calling
    private volatile boolean hasMore = false;

    private final PrefetchRepoComparator repoComparator = new PrefetchRepoComparator();

    private final Locker<String> mutex = new Locker<>();

    private static final String MUTEX_KEY = "mutex";

    @Inject
    private Instance<ContentListBuilder> listBuilders;

    void initRepoCache()
    {
        lockAnd( t -> {
            if ( !resourceCache.isEmpty() )
            {
                for ( RemoteRepository repo : resourceCache.execute( c -> c.keySet() ) )
                {
                    if ( !repoQueue.contains( repo ) )
                    {
                        repoQueue.add( repo );
                    }
                }
                sortRepoQueue();
            }
            hasMore = !repoQueue.isEmpty() && !resourceCache.isEmpty();
            return null;
        } );
    }

    public void scheduleRepo( final RemoteRepository repo, final List<RescanablePath> paths )
    {
        if ( shouldSchedule )
        {
            lockAnd( t -> {
                if ( !repoQueue.contains( repo ) )
                {
                    repoQueue.add( repo );
                    sortRepoQueue();
                }

                List<RescanablePath> repoPaths = resourceCache.get( repo );

                if ( repoPaths == null )
                {
                    repoPaths = new ArrayList<>( paths.size() );
                    resourceCache.put( repo, repoPaths );
                }
                repoPaths.addAll( paths );
                hasMore = !repoQueue.isEmpty() && !resourceCache.isEmpty();
                return null;
            } );
        }
    }

    public void rescheduleForRescan()
    {
        if ( shouldSchedule && !hasMore )
        {
            lockAnd( t -> {
                for ( RemoteRepository repo : repoQueue )
                {
                    if ( repo.isPrefetchRescan() )
                    {
                        String rescanTime = repo.getPrefetchRescanTimestamp();
                        logger.trace( "repo's current rescan time: {}", rescanTime );
                        if ( StringUtils.isBlank( rescanTime ) || isNowAfter( rescanTime ) )
                        {
                            repo.setPrefetchRescanTimestamp(
                                    getNextRescanTimeFromNow( config.getRescanIntervalSeconds() ) );
                            try
                            {
                                // Will not send store update event to avoid recursive rescheduling
                                storeDataManager.storeArtifactStore( repo, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                                                              "Update store for prefetch rescan update" ),
                                                                     false, false, new EventMetadata() );
                            }
                            catch ( IndyDataException e )
                            {
                                logger.error( String.format( "Can not update store in prefetching rescan for repo: %s",
                                                             repo ), e );
                            }
                            logger.trace( "Rescan time set. Repo's next rescan time: {}", repo.getPrefetchRescanTimestamp() );
                            final boolean isScheduledRescan =
                                    StringUtils.isNotBlank( rescanTime ) && isNowAfter( rescanTime );
                            if ( isScheduledRescan )
                            {
                                List<RescanablePath> rootPaths = buildPaths( repo, true );
                                logger.trace( "Schedule rescan enabled resources: repo: {}, paths {}", repo,
                                              rootPaths );
                                scheduleRepo( repo, rootPaths );
                            }
                        }
                        break;
                    }
                }

                return null;
            } );
        }
    }

    public Map<RemoteRepository, List<RescanableResourceWrapper>> remove( final int size )
    {
        return lockAnd( t -> {
            Map<RemoteRepository, List<RescanableResourceWrapper>> resources = new HashMap<>( 2 );
            int removedSize = 0;
            final List<RemoteRepository> repoQueueCopy = new ArrayList<>( repoQueue );
            for ( RemoteRepository repo : repoQueueCopy )
            {
                List<RescanablePath> paths = resourceCache.get( repo );
                if ( paths != null )
                {
                    List<RescanableResourceWrapper> res = new ArrayList<>( size );
                    List<RescanablePath> pathsRemoved = new ArrayList<>( size );
                    for ( RescanablePath path : paths )
                    {
                        res.add( new RescanableResourceWrapper(
                                new StoreResource( LocationUtils.toLocation( repo ), path.getPath() ),
                                path.isRescan() ) );
                        pathsRemoved.add( path );
                        if ( ++removedSize >= size )
                        {
                            break;
                        }
                    }
                    resources.put( repo, res );

                    paths.removeAll( pathsRemoved );

                    if ( paths.isEmpty() )
                    {
                        resourceCache.remove( repo );
                        if ( !repo.isPrefetchRescan() )
                        {
                            repoQueue.remove( repo );
                            sortRepoQueue();
                        }
                        hasMore = !repoQueue.isEmpty() && !resourceCache.isEmpty();
                    }

                    if ( removedSize >= size )
                    {
                        return resources;
                    }
                }
                else
                {
                    if ( !repo.isPrefetchRescan() )
                    {
                        repoQueue.remove( repo );
                        sortRepoQueue();
                    }
                }
            }
            return resources;
        } );
    }

    public Map<RemoteRepository, List<ConcreteResource>> get( final int size )
    {
        return lockAnd( t -> {
            Map<RemoteRepository, List<ConcreteResource>> resources = new HashMap<>( 2 );
            int removedSize = 0;
            for ( RemoteRepository repo : repoQueue )
            {
                List<RescanablePath> paths = resourceCache.get( repo );
                if ( paths != null && !paths.isEmpty() )
                {
                    List<ConcreteResource> res = new ArrayList<>( size );
                    for ( RescanablePath path : paths )
                    {
                        res.add( new StoreResource( LocationUtils.toLocation( repo ), path.getPath() ) );
                    }
                    resources.put( repo, res );
                    if ( removedSize >= size )
                    {
                        return resources;
                    }
                }
            }
            return resources;
        } );
    }

    public boolean hasMore()
    {
        return hasMore;
    }

    private void sortRepoQueue()
    {
        if ( repoQueue.size() > 1 )
        {
            repoQueue.sort( repoComparator );
        }
    }

    private <T> T lockAnd( Function<String, T> function )
    {
        return mutex.lockAnd( MUTEX_KEY, Integer.MAX_VALUE, function, ( k, lock ) -> true );
    }

    List<RescanablePath> buildPaths( final RemoteRepository repository, final boolean isRescan )
    {
        for ( ContentListBuilder builder : listBuilders )
        {
            if ( repository.getPrefetchListingType().equals( builder.type() ) )
            {
                logger.trace( "Use {} for {}", builder, repository.getName() );
                return builder.buildPaths( repository, isRescan );
            }
        }

        // By default, we will use html content list builder if no builder matched.
        return new HtmlContentListBuilder().buildPaths( repository, isRescan );
    }

    public void stopSchedulingMore()
    {
        shouldSchedule = false;
    }

    public void stop(){
        stopSchedulingMore();
        resourceCache.stop();
    }

}
