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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.model.core.StoreKey;

@ApiModel( description = "Listing of paths that could not be retrieved for a given artifact store", value = "not-found cache section" )
public class NotFoundCacheSectionDTO
{

    @ApiModelProperty( required = true, dataType = "string", value = "Serialized store key, of the form: '[hosted|group|remote]:name'" )
    private StoreKey key;

    @ApiModelProperty( required = true, value = "paths that failed retrieval within this store (may be empty)" )
    private Set<String> paths;

    public NotFoundCacheSectionDTO(){}

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

    public void setKey( StoreKey key )
    {
        this.key = key;
    }

    public void setPaths( Set<String> paths )
    {
        this.paths = paths;
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
