package org.commonjava.aprox.model.core.dto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.model.core.StoreKey;

public class NotFoundCacheSectionDTO
{

    private final StoreKey key;

    private final Set<String> paths;

    public NotFoundCacheSectionDTO( final StoreKey key, final Collection<String> paths )
    {
        this.key = key;
        this.paths = ( paths instanceof Set ) ? (Set<String>) paths : new HashSet<String>( paths );
    }

    public StoreKey getKey()
    {
        return key;
    }

    public Set<String> getPaths()
    {
        return paths;
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
        final NotFoundCacheSectionDTO other = (NotFoundCacheSectionDTO) obj;
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
