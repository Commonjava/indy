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
package org.commonjava.indy.content;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.dto.DirectoryListingEntryDTO;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.maven.galley.model.ConcreteResource;

/**
 * Implementation of {@link ConcreteResource} which assumes locations are {@link KeyedLocation} instances, and provides access to the {@link StoreKey}s
 * they hold. Also provides a method for converting a list of these resources into a listing DTO.
 */
public class StoreResource
    extends ConcreteResource
{

    public StoreResource( final KeyedLocation location, final String... path )
    {
        super( location, path );
    }

    /**
     * Retrieve the {@link StoreKey} from the {@link KeyedLocation} referenced by this resource.
     */
    public StoreKey getStoreKey()
    {
        return ( (KeyedLocation) getLocation() ).getKey();
    }

    /**
     * Convert a series of {@link StoreResource}s into a DTO for use in generating browser or other directory listings.
     */
    public static List<DirectoryListingEntryDTO> convertToEntries( final List<StoreResource> items )
    {
        final List<DirectoryListingEntryDTO> entries = new ArrayList<DirectoryListingEntryDTO>();
        for ( final StoreResource resource : items )
        {
            entries.add( new DirectoryListingEntryDTO( resource.getStoreKey(), resource.getPath() ) );
        }
        return entries;
    }

}
