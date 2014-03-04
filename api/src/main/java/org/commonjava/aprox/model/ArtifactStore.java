/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class ArtifactStore
    implements Serializable
{

    private static final long serialVersionUID = 1L;

    private StoreKey key;

    private transient Map<String, String> metadata;

    protected ArtifactStore()
    {
    }

    protected ArtifactStore( final String name )
    {
        this.key = initKey( name );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.web.maven.proxy.model.ArtifactStore#getName()
     */
    public String getName()
    {
        return key.getName();
    }

    protected void setName( final String name )
    {
        this.key = initKey( name );
    }

    @Deprecated
    public StoreType getDoctype()
    {
        return getKey().getType();
    }

    public synchronized StoreKey getKey()
    {
        return key;
    }

    protected abstract StoreKey initKey( String name );

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
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
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ArtifactStore other = (ArtifactStore) obj;
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

    public synchronized String setMetadata( final String key, final String value )
    {
        if ( key == null || value == null )
        {
            return null;
        }

        if ( metadata == null )
        {
            metadata = new HashMap<String, String>();
        }

        return metadata.put( key, value );
    }

    public String getMetadata( final String key )
    {
        return metadata == null ? null : metadata.get( key );
    }

    @Override
    public String toString()
    {
        return String.format( "ArtifactStore [key=%s]", key );
    }

}
