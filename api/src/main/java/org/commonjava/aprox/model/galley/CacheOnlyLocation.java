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
package org.commonjava.aprox.model.galley;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.maven.galley.spi.cache.CacheProvider;

public class CacheOnlyLocation
    implements KeyedLocation
{

    private final HostedRepository repo;

    private final Map<String, Object> attributes = new HashMap<String, Object>();

    private final StoreKey key;

    public CacheOnlyLocation( final HostedRepository repo )
    {
        this.repo = repo;
        if ( repo.getStorage() != null )
        {
            attributes.put( CacheProvider.ATTR_ALT_STORAGE_LOCATION, repo.getStorage() );
        }

        this.key = repo.getKey();
    }

    public CacheOnlyLocation( final StoreKey key )
    {
        this.repo = null;
        this.key = key;
    }

    public boolean hasDeployPoint()
    {
        return repo != null;
    }

    @Override
    public boolean allowsPublishing()
    {
        return false;
    }

    @Override
    public boolean allowsStoring()
    {
        return true;
    }

    @Override
    public boolean allowsSnapshots()
    {
        return repo == null ? false : repo.isAllowSnapshots();
    }

    @Override
    public boolean allowsReleases()
    {
        return repo == null ? true : repo.isAllowReleases();
    }

    @Override
    public String getUri()
    {
        return "aprox:" + key;
    }

    @Override
    public int getTimeoutSeconds()
    {
        return 0;
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    @Override
    public <T> T getAttribute( final String key, final Class<T> type )
    {
        final Object value = attributes.get( key );
        return value == null ? null : type.cast( value );
    }

    @Override
    public Object removeAttribute( final String key )
    {
        return attributes.remove( key );
    }

    @Override
    public Object setAttribute( final String key, final Object value )
    {
        return attributes.put( key, value );
    }

    @Override
    public StoreKey getKey()
    {
        return key;
    }

    @Override
    public boolean allowsDownloading()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "Cache-only location [" + key + "]";
    }

    @Override
    public String getName()
    {
        return getKey().toString();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( key == null ) ? 0 : key.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final CacheOnlyLocation other = (CacheOnlyLocation) obj;
        if ( key == null )
        {
            if ( other.key != null )
            {
                return false;
            }
        }
        else if ( !key.equals( other.key ) )
        {
            return false;
        }
        return true;
    }

}
