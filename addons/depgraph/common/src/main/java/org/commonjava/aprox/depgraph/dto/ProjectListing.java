package org.commonjava.aprox.depgraph.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;

public class ProjectListing<T extends ProjectRef>
{

    private List<T> items;

    public ProjectListing()
    {
    }

    public ProjectListing( final Collection<T> items )
    {
        this.items = ( items instanceof List ? (List<T>) items : new ArrayList<>( items ) );
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
