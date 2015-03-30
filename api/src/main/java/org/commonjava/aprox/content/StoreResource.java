package org.commonjava.aprox.content;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.dto.DirectoryListingEntryDTO;
import org.commonjava.aprox.model.galley.KeyedLocation;
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
