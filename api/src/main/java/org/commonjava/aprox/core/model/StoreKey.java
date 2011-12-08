/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.model;


public final class StoreKey
{
    // private static final Logger logger = new Logger( StoreKey.class );

    private final StoreType type;

    private final String name;

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
            type = StoreType.repository;
        }
        else
        {
            name = id.substring( idx + 1 );
            type = StoreType.valueOf( id.substring( 0, idx ) );
        }

        // logger.info( "parsed store-key with type: '%s' and name: '%s'", type, name );

        return new StoreKey( type, name );
    }
}
