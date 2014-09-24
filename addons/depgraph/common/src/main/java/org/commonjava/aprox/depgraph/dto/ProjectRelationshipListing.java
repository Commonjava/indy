package org.commonjava.aprox.depgraph.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

public class ProjectRelationshipListing
{

    private List<ProjectRelationship<?>> items;

    public ProjectRelationshipListing()
    {
    }

    public ProjectRelationshipListing( final Collection<ProjectRelationship<?>> items )
    {
        this.items = ( items instanceof List ? (List<ProjectRelationship<?>>) items : new ArrayList<>( items ) );
    }

    public List<ProjectRelationship<?>> getItems()
    {
        return items;
    }

    public void setItems( final List<ProjectRelationship<?>> items )
    {
        this.items = items;
    }

}
