package org.commonjava.aprox.depgraph.dto;

import org.commonjava.maven.atlas.graph.RelationshipGraph;

public class GraphWorkspaceDTO
{

    private final String id;

    public GraphWorkspaceDTO( final RelationshipGraph graph )
    {
        this.id = graph.getWorkspaceId();
    }

    public String getId()
    {
        return id;
    }

}
