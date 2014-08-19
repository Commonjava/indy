package org.commonjava.aprox.core.dto;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.content.StoreResource;

public class DirectoryListingDTO
{

    private final List<DirectoryListingEntryDTO> items;

    public DirectoryListingDTO( final List<StoreResource> items )
    {
        this.items = convertToEntries( items );
    }

    private List<DirectoryListingEntryDTO> convertToEntries( final List<StoreResource> items )
    {
        final List<DirectoryListingEntryDTO> entries = new ArrayList<DirectoryListingEntryDTO>();
        for ( final StoreResource resource : items )
        {
            entries.add( new DirectoryListingEntryDTO( resource.getStoreKey(), resource.getPath() ) );
        }
        return entries;
    }

    public List<DirectoryListingEntryDTO> getItems()
    {
        return items;
    }

}
