package org.commonjava.aprox.model.core.dto;

import java.util.List;

public class DirectoryListingDTO
{

    private final List<DirectoryListingEntryDTO> items;

    public DirectoryListingDTO( final List<DirectoryListingEntryDTO> items )
    {
        this.items = items;
    }

    public List<DirectoryListingEntryDTO> getItems()
    {
        return items;
    }

}
