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
package org.commonjava.indy.core.inject;

import static org.commonjava.maven.galley.util.PathUtils.normalize;

import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.model.galley.RepositoryLocation;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Default
//@Production
public class ExpiringMemoryNotFoundCache
    implements NotFoundCache
{

    private static final String TIMEOUT_FORMAT = "yyyy-MM-dd hh:mm:ss z";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final ScheduledExecutorService evictionService = Executors.newScheduledThreadPool( 1 );

    @Inject
    protected IndyConfiguration config;

    protected final Map<ResourceSoftReference<ConcreteResource>, Long> missingWithTimeout = new HashMap<>();

    protected ExpiringMemoryNotFoundCache()
    {
        // schedule to run eviction after 8 hours and every 8 hours
        evictionService.scheduleAtFixedRate( () ->
                                       {
                                           clearAllExpiredMissing();
                                       }, 8, 8, TimeUnit.HOURS );
    }

    public ExpiringMemoryNotFoundCache( final IndyConfiguration config )
    {
        this();
        this.config = config;
    }

    private class ResourceSoftReference<T>
                    extends SoftReference<T>
    {
        public ResourceSoftReference( T referent )
        {
            super( referent );
        }

        @Override
        public boolean equals( Object other )
        {

            boolean returnValue = super.equals( other );

            // If we're not equal, then check equality using referenced objects
            if ( !returnValue && ( other instanceof ResourceSoftReference<?> ) )
            {
                T value = this.get();
                if ( value != null )
                {
                    T otherValue = ( (ResourceSoftReference<T>) other ).get();

                    // The delegate equals should handle otherValue == null
                    returnValue = value.equals( otherValue );
                }
            }
            return returnValue;
        }

        @Override
        public int hashCode()
        {
            T value = this.get();
            return value != null ? value.hashCode() : super.hashCode();
        }
    }

    @Override
    public void addMissing( final ConcreteResource resource )
    {
        long timeout = Long.MAX_VALUE;
        if ( config.getNotFoundCacheTimeoutSeconds() > 0 )
        {
            timeout = System.currentTimeMillis() + config.getNotFoundCacheTimeoutSeconds() * 1000;
        }

        final Location loc = resource.getLocation();
        final Integer to = loc.getAttribute( RepositoryLocation.ATTR_NFC_TIMEOUT_SECONDS, Integer.class );
        if ( to != null && to > 0 )
        {
            timeout = System.currentTimeMillis() + ( to * 1000 );
        }

        final long tstamp = timeout;
        logger.info( "[NFC] '{}' will not be checked again until: {}", new Object()
        {
            @Override
            public String toString()
            {
                return normalize( resource.getLocationUri(), resource.getPath() );
            }
        }, new Object()
        {
            @Override
            public String toString()
            {
                return new SimpleDateFormat( TIMEOUT_FORMAT ).format( new Date( tstamp ) );
            }
        } );

        synchronized ( MUTEX )
        {
            missingWithTimeout.put( new ResourceSoftReference( resource ), timeout );
        }
    }

    @Override
    public boolean isMissing( final ConcreteResource resource )
    {
        Long timeout;
        synchronized ( MUTEX )
        {
            timeout = missingWithTimeout.get( new ResourceSoftReference( resource ) );
        }
        boolean result = false;
        if ( timeout != null && System.currentTimeMillis() < timeout )
        {
            result = true;
        }

        logger.debug( "NFC check: {} result is: {}", resource, result );
        return result;
    }

    @Override
    public void clearMissing( final Location location )
    {
        synchronized ( MUTEX )
        {
            // The set is backed by the map, so changes to the map are reflected in the set, and vice-versa.
            for ( Iterator<ResourceSoftReference<ConcreteResource>>
                  i = missingWithTimeout.keySet().iterator(); i.hasNext(); )
            {
                ResourceSoftReference<ConcreteResource> resourceRef = i.next();
                ConcreteResource resource = resourceRef.get();
                if ( resource == null || resource.getLocation().equals( location ) )
                {
                    i.remove();
                }
            }
        }
    }

    @Override
    public void clearMissing( final ConcreteResource resource )
    {
        synchronized ( MUTEX )
        {
            missingWithTimeout.remove( new ResourceSoftReference( resource ) );
        }
    }

    @Override
    public void clearAllMissing()
    {
        synchronized ( MUTEX )
        {
            this.missingWithTimeout.clear();
        }
    }

    @Override
    public Map<Location, Set<String>> getAllMissing()
    {
        final Map<Location, Set<String>> result = new HashMap<>();
        Set<ResourceSoftReference<ConcreteResource>> keySet = getKeySet();

        for ( final ResourceSoftReference<ConcreteResource> resourceRef : keySet )
        {
            ConcreteResource resource = resourceRef.get();
            if ( resource == null )
            {
                continue;
            }
            final Location loc = resource.getLocation();
            Set<String> paths = result.computeIfAbsent( loc, k -> new HashSet<>() );
            paths.add( resource.getPath() );
        }
        return result;
    }

    @Override
    public Set<String> getMissing( final Location location )
    {
        final Set<String> paths = new HashSet<>();
        Set<ResourceSoftReference<ConcreteResource>> keySet = getKeySet();

        for ( final ResourceSoftReference<ConcreteResource> resourceRef : keySet )
        {
            ConcreteResource resource = resourceRef.get();
            if ( resource == null )
            {
                continue;
            }
            final Location loc = resource.getLocation();
            if ( loc.equals( location ) )
            {
                paths.add( resource.getPath() );
            }
        }
        return paths;
    }

    // One million. We can not return a huge collection without a memory issue when calling getAllMissing/getMissing
    private final static int MAX_SIZE_AFFORDABLE = 1_000_000;

    private Set<ResourceSoftReference<ConcreteResource>> getKeySet()
    {
        synchronized ( MUTEX )
        {
            int size = missingWithTimeout.size();
            if ( size > MAX_SIZE_AFFORDABLE )
            {
                logger.warn( "Can not retrieve all NFC entries - size too big, {}", size );
                return Collections.emptySet();
            }
            return new HashSet<>( missingWithTimeout.keySet() );
        }
    }

    private final Object MUTEX = new Object(); // we must synchronize when we iterate through the map or remove entries

    /**
     * This is time consuming. We set up a scheduled task to to it.
     */
    private void clearAllExpiredMissing()
    {
        synchronized ( MUTEX )
        {
            for ( final Iterator<Map.Entry<ResourceSoftReference<ConcreteResource>, Long>>
                  it = missingWithTimeout.entrySet().iterator(); it.hasNext(); )
            {
                final Map.Entry<ResourceSoftReference<ConcreteResource>, Long> entry = it.next();
                ConcreteResource resource = entry.getKey().get();
                if ( resource == null )
                {
                    it.remove();
                }
                else
                {
                    final Long timeout = entry.getValue();
                    if ( System.currentTimeMillis() >= timeout )
                    {
                        it.remove();
                    }
                }
            }
        }
    }

}
