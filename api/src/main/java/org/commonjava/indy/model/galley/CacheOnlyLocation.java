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
package org.commonjava.indy.model.galley;

import org.commonjava.indy.content.IndyLocationExpander;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link KeyedLocation} implementation that only knows about locally hosted/cached content. During Galley's handling, it can be converted into other 
 * store-related {@link KeyedLocation} types, assuming it's not referencing a {@link HostedRepository}, via the {@link IndyLocationExpander} component.
 */
public class CacheOnlyLocation
    implements KeyedLocation
{

    private final Map<String, Object> attributes = new HashMap<String, Object>();

    private final StoreKey key;

    private final boolean isHosted;

    private final boolean isAllowSnapshots;

    private final boolean isAllowReleases;

    private boolean isReadOnly;

    /**
     * This is used to bypass the repo's readonly flag in case of internal workflow like promotion.
     */
    public void setReadonly( boolean isReadOnly )
    {
        this.isReadOnly = isReadOnly;
    }

    public CacheOnlyLocation( final HostedRepository repo )
    {
        this.isHosted = true;
        this.isAllowReleases = repo.isAllowReleases();
        this.isAllowSnapshots = repo.isAllowSnapshots();
        this.isReadOnly = repo.isReadonly();

        if ( repo.getStorage() != null )
        {
            attributes.put( Location.ATTR_ALT_STORAGE_LOCATION, repo.getStorage() );
        }

        this.key = repo.getKey();
    }

    public CacheOnlyLocation( final StoreKey key )
    {
        this.isHosted = false;
        this.isAllowReleases = true;
        this.isAllowSnapshots = false;
        this.isReadOnly = true;

        this.key = key;
    }

    public boolean isHostedRepository()
    {
        return isHosted;
    }

    @Override
    public boolean allowsPublishing()
    {
        return false;
    }

    @Override
    public boolean allowsStoring()
    {
        return isHosted && !isReadOnly;
    }

    @Override
    public boolean allowsSnapshots()
    {
        return isAllowSnapshots;
    }

    @Override
    public boolean allowsReleases()
    {
        return isAllowReleases;
    }

    @Override
    public boolean allowsDeletion()
    {
        return isHosted && !isReadOnly;
    }

    @Override
    public String getUri()
    {
        return "indy:" + key;
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    @Override
    public <T> T getAttribute( final String key, final Class<T> type )
    {
        return getAttribute( key, type, null );
    }

    @Override
    public <T> T getAttribute( final String key, final Class<T> type, final T defaultValue )
    {
        final Object value = attributes.get( key );
        return value == null ? defaultValue : type.cast( value );
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
