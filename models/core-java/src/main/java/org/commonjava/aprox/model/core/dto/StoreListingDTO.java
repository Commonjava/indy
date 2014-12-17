package org.commonjava.aprox.model.core.dto;

import java.util.List;

import org.commonjava.aprox.model.core.ArtifactStore;

public class StoreListingDTO<T extends ArtifactStore>
{

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
        return items;
    }

    public void setItems( final List<T> items )
    {
        this.items = items;
    }

}
