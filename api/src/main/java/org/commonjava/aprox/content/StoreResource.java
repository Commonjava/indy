package org.commonjava.aprox.content;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.dto.DirectoryListingEntryDTO;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.maven.galley.model.ConcreteResource;

public class StoreResource
    extends ConcreteResource
{

    public StoreResource( final KeyedLocation location, final String... path )
    {
        super( location, path );
    }

    public StoreKey getStoreKey()
    {
        return ( (KeyedLocation) getLocation() ).getKey();
    }

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
