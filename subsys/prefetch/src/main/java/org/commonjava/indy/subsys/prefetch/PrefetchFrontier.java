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

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApplicationScoped
public class PrefetchFrontier
{
    private List<PrioritizedResource> resourceQueue = new ArrayList<>();

    private volatile boolean shouldSchedule = true;

    private static final Comparator<PrioritizedResource> comparator = ( r1, r2 ) -> {
        if ( r1 == null )
        {
            return 1;
        }
        if ( r2 == null )
        {
            return -1;
        }
        return r2.getPriority() - r1.getPriority();
    };

    private final ReentrantLock mutex = new ReentrantLock();

    public void scheduleResources( final List<PrioritizedResource> resources )
    {
        if ( shouldSchedule )
        {
            lockAnd( () -> {
                resourceQueue.addAll( resources.stream().filter( Objects::nonNull ).collect( Collectors.toList() ) );
                if ( resourceQueue.size() >= 2 )
                {
                    resourceQueue.sort( comparator );
                }
                return null;
            } );
        }
    }

    public List<PrioritizedResource> remove( final int size )
    {
        return lockAnd( () -> {
            List<PrioritizedResource> resources = new ArrayList<>( size );
            for ( int i = 0; i < size; i++ )
            {
                if ( !resourceQueue.isEmpty() )
                {
                    resources.add( resourceQueue.remove( 0 ) );
                }
                else
                {
                    break;
                }
            }
            return resources;
        } );
    }

    public List<PrioritizedResource> get( final int size )
    {
        return lockAnd( () -> {
            List<PrioritizedResource> resources = new ArrayList<>( size );
            for ( int i = 0; i < size; i++ )
            {
                if ( !resourceQueue.isEmpty() )
                {
                    resources.add( resourceQueue.get( i ) );
                }
                else
                {
                    break;
                }
            }
            return resources;
        } );
    }

    public boolean hasMore()
    {
        return lockAnd( () -> !resourceQueue.isEmpty() );
    }

    private <T> T lockAnd( Supplier<T> action )
    {
        try
        {
            mutex.lock();
            return action.get();
        }
        finally
        {
            if ( mutex.isLocked() )
            {
                mutex.unlock();
            }
        }
    }

    public void stopSchedulingMore()
    {
        shouldSchedule = false;
    }

}
