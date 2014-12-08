package org.commonjava.aprox.core.dto;

import org.commonjava.aprox.model.core.StoreKey;

public class DirectoryListingEntryDTO
{

    private final StoreKey key;

    private final String path;

    public DirectoryListingEntryDTO( final StoreKey key, final String path )
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
        final DirectoryListingEntryDTO other = (DirectoryListingEntryDTO) obj;
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

    @Override
    public String toString()
    {
        return String.format( "%s/%s", key, path );
    }

}
