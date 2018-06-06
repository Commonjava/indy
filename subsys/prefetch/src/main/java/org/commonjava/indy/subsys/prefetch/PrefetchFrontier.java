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

import org.commonjava.cdi.util.weft.Locker;
import org.commonjava.indy.content.StoreResource;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
public class PrefetchFrontier
{
    @Inject
    @PrefetchCache
    private CacheHandle<RemoteRepository, List> resourceCache;

    private final List<RemoteRepository> repoQueue = new ArrayList<>();

    private volatile boolean shouldSchedule = true;

    // Use this volatile to avoid lock on hasMore calling
    private volatile boolean hasMore = false;

    private static final Comparator<RemoteRepository> repoComparator = ( r1, r2 ) -> {
        if ( r1 == null )
        {
            return 1;
        }
        if ( r2 == null )
        {
            return -1;
        }
        return r2.getPrefetchPriority() - r1.getPrefetchPriority();
    };

    private final Locker<String> mutex = new Locker<>();

    private static final String MUTEX_KEY = "mutex";

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

    public void scheduleRepo( final RemoteRepository repo, final List<String> paths )
    {
        if ( shouldSchedule )
        {
            lockAnd( t -> {
                if ( !repoQueue.contains( repo ) )
                {
                    repoQueue.add( repo );
                    sortRepoQueue();
                }

                List<String> repoPaths = resourceCache.get( repo );

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

    public Map<RemoteRepository, List<ConcreteResource>> remove( final int size )
    {
        return lockAnd( t -> {
            Map<RemoteRepository, List<ConcreteResource>> resources = new HashMap<>( 2 );
            int removedSize = 0;
            final List<RemoteRepository> repoQueueCopy = new ArrayList<>( repoQueue );
            for ( RemoteRepository repo : repoQueueCopy )
            {
                List<String> paths = resourceCache.get( repo );
                List<ConcreteResource> res = new ArrayList<>( size );
                List<String> pathsRemoved = new ArrayList<>( size );
                for ( String path : paths )
                {
                    res.add( new StoreResource( LocationUtils.toLocation( repo ), path ) );
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
                    repoQueue.remove( repo );
                    sortRepoQueue();
                    hasMore = !repoQueue.isEmpty() && !resourceCache.isEmpty();
                }

                if ( removedSize >= size )
                {
                    return resources;
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
                List<String> paths = resourceCache.get( repo );
                List<ConcreteResource> res = new ArrayList<>( size );
                for ( String path : paths )
                {
                    res.add( new StoreResource( LocationUtils.toLocation( repo ), path ) );
                }
                resources.put( repo, res );
                if ( removedSize >= size )
                {
                    return resources;
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

    public void stopSchedulingMore()
    {
        shouldSchedule = false;
    }

}
