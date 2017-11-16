/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
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
@Alternative
public class ExpiringMemoryNotFoundCache
    implements NotFoundCache
{

    private static final String TIMEOUT_FORMAT = "yyyy-MM-dd hh:mm:ss z";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected IndyConfiguration config;

    // TODO: Now using a simple hashmap, need to take attention here to see if need ISPN instead if it is a mem eater.
    protected final Map<ConcreteResource, Long> missingWithTimeout = new HashMap<>();

    protected ExpiringMemoryNotFoundCache()
    {
    }

    public ExpiringMemoryNotFoundCache( final IndyConfiguration config )
    {
        this.config = config;
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

        missingWithTimeout.put( resource, timeout );
    }

    @Override
    public boolean isMissing( final ConcreteResource resource )
    {
        final Long timeout = missingWithTimeout.get( resource );
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
        for ( final ConcreteResource resource : new HashSet<>( missingWithTimeout.keySet() ) )
        {
            if ( resource.getLocation()
                         .equals( location ) )
            {
                missingWithTimeout.remove( resource );
            }
        }
    }

    @Override
    public void clearMissing( final ConcreteResource resource )
    {
        missingWithTimeout.remove( resource );
    }

    @Override
    public void clearAllMissing()
    {
        this.missingWithTimeout.clear();
    }

    @Override
    public Map<Location, Set<String>> getAllMissing()
    {
        clearAllExpiredMissing();
        final Map<Location, Set<String>> result = new HashMap<>();
        for ( final ConcreteResource resource : missingWithTimeout.keySet() )
        {
            final Location loc = resource.getLocation();
            Set<String> paths = result.computeIfAbsent( loc, k -> new HashSet<>() );

            paths.add( resource.getPath() );
        }

        return result;
    }

    @Override
    public Set<String> getMissing( final Location location )
    {
        clearAllExpiredMissing();
        final Set<String> paths = new HashSet<>();
        for ( final ConcreteResource resource : missingWithTimeout.keySet() )
        {
            final Location loc = resource.getLocation();
            if ( loc.equals( location ) )
            {
                paths.add( resource.getPath() );
            }
        }

        return paths;
    }

    private void clearAllExpiredMissing()
    {
        for ( final Iterator<Map.Entry<ConcreteResource, Long>> it = missingWithTimeout.entrySet()
                                                                                       .iterator(); it.hasNext(); )
        {
            final Map.Entry<ConcreteResource, Long> entry = it.next();
            final Long timeout = entry.getValue();

            if ( System.currentTimeMillis() >= timeout )
            {
                it.remove();
            }
        }
    }

}
