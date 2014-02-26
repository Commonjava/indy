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

public final class StoreKey
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    // private static final Logger logger = new Logger( StoreKey.class );

    private final StoreType type;

    private final String name;

    protected StoreKey()
    {
        this.type = null;
        this.name = null;
    }

    public StoreKey( final StoreType type, final String name )
    {
        this.type = type;
        this.name = name;
    }

    public StoreType getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return type.name() + ":" + name;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
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
        final StoreKey other = (StoreKey) obj;
        if ( name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !name.equals( other.name ) )
        {
            return false;
        }
        if ( type != other.type )
        {
            return false;
        }
        return true;
    }

    public static StoreKey fromString( final String id )
    {
        final int idx = id.indexOf( ':' );

        String name;
        StoreType type;
        if ( idx < 1 )
        {
            name = id;
            type = StoreType.remote;
        }
        else
        {
            name = id.substring( idx + 1 );
            type = StoreType.valueOf( id.substring( 0, idx ) );
        }

        // logger.info( "parsed store-key with type: '{}' and name: '{}'", type, name );

        return new StoreKey( type, name );
    }
}
