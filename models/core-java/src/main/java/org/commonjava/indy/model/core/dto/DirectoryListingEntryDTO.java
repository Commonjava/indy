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
package org.commonjava.indy.model.core.dto;

import org.commonjava.indy.model.core.StoreKey;

public class DirectoryListingEntryDTO
{

    private StoreKey key;

    private String path;

    public DirectoryListingEntryDTO(){}

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

    public void setKey( StoreKey key )
    {
        this.key = key;
    }

    public void setPath( String path )
    {
        this.path = path;
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
