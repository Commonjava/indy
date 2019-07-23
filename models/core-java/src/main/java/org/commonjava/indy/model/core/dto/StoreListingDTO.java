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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.model.core.ArtifactStore;

@ApiModel( "List of artifact store definitions" )
public class StoreListingDTO<T extends ArtifactStore>
    implements Iterable<T>
{

    @ApiModelProperty( dataType = "org.commonjava.indy.model.core.ArtifactStore", required = true, value = "The store definition list" )
    private List<T> items;

    public StoreListingDTO()
    {
    }

    public StoreListingDTO( final List<T> items )
    {
        this.items = items;
    }

    public List<T> getItems()
    {
        return items == null ? Collections.emptyList() : items;
    }

    public void setItems( final List<T> items )
    {
        this.items = items;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "StoreListingDTO[" );
        if ( items == null || items.isEmpty() )
        {
            sb.append( "NO STORES" );
        }
        else
        {
            for ( final T item : items )
            {
                sb.append( "\n  " )
                  .append( item );
            }
        }

        sb.append( "\n]" );
        return sb.toString();
    }

    @Override
    public Iterator<T> iterator()
    {
        return getItems().iterator();
    }

}
