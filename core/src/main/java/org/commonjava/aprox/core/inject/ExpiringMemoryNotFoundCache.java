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
package org.commonjava.aprox.core.inject;

import static org.commonjava.maven.galley.util.PathUtils.normalize;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.galley.RepositoryLocation;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Default
@Production
public class ExpiringMemoryNotFoundCache
    implements NotFoundCache
{

    private static final String TIMEOUT_FORMAT = "yyyy-MM-dd hh:mm:ss z";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    protected AproxConfiguration config;

    protected final Map<ConcreteResource, Long> missingWithTimeout = new HashMap<ConcreteResource, Long>();

    protected ExpiringMemoryNotFoundCache()
    {
    }

    public ExpiringMemoryNotFoundCache( final AproxConfiguration config )
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
        if ( timeout != null )
        {
            if ( System.currentTimeMillis() >= timeout )
            {
                missingWithTimeout.remove( resource );
                return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public void clearMissing( final Location location )
    {
        for ( final ConcreteResource resource : new HashSet<ConcreteResource>( missingWithTimeout.keySet() ) )
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
        final Map<Location, Set<String>> result = new HashMap<Location, Set<String>>();
        for ( final ConcreteResource resource : missingWithTimeout.keySet() )
        {
            final Location loc = resource.getLocation();
            Set<String> paths = result.get( loc );
            if ( paths == null )
            {
                paths = new HashSet<String>();
                result.put( loc, paths );
            }

            paths.add( resource.getPath() );
        }

        return result;
    }

    @Override
    public Set<String> getMissing( final Location location )
    {
        clearAllExpiredMissing();
        final Set<String> paths = new HashSet<String>();
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
