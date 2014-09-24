package org.commonjava.aprox.core.expire;

import org.commonjava.aprox.model.StoreKey;

public class ContentExpiration
{

    private StoreKey key;

    private String path;

    protected ContentExpiration()
    {
    }

    public ContentExpiration( final StoreKey key, final String path )
    {
        this.key = key;
        this.path = path;
    }

    public StoreKey getKey()
    {
        return key;
    }

    public String getPath()
    {
        return path;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( key == null ) ? 0 : key.hashCode() );
        result = prime * result + ( ( path == null ) ? 0 : path.hashCode() );
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
        final ContentExpiration other = (ContentExpiration) obj;
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
        if ( path == null )
        {
            if ( other.path != null )
            {
                return false;
            }
        }
        else if ( !path.equals( other.path ) )
        {
            return false;
        }
        return true;
    }

    protected void setKey( final StoreKey key )
    {
        this.key = key;
    }

    protected void setPath( final String path )
    {
        this.path = path;
    }

}
