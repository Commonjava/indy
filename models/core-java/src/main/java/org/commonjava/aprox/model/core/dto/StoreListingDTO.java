package org.commonjava.aprox.model.core.dto;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.commonjava.aprox.model.core.ArtifactStore;

public class StoreListingDTO<T extends ArtifactStore>
    implements Iterable<T>
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
        return items == null ? Collections.<T> emptyList() : items;
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
