package org.commonjava.aprox.model.galley;

import java.util.HashMap;
import java.util.Map;

import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.StoreKey;

public class CacheOnlyLocation
    implements KeyedLocation
{

    private final DeployPoint deploy;

    private final Map<String, Object> attributes = new HashMap<>();

    private final StoreKey key;

    public CacheOnlyLocation( final DeployPoint deploy )
    {
        this.deploy = deploy;
        this.key = deploy.getKey();
    }

    public CacheOnlyLocation( final StoreKey key )
    {
        this.deploy = null;
        this.key = key;
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
        return deploy == null ? false : deploy.isAllowSnapshots();
    }

    @Override
    public boolean allowsReleases()
    {
        return deploy == null ? true : deploy.isAllowReleases();
    }

    @Override
    public String getUri()
    {
        return null;
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
        return key.toString();
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
