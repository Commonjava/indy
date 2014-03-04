package org.commonjava.aprox.depgraph.dto;

import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphTransferDTO
{

    private Set<ProjectVersionRef> roots;

    private Set<ProjectRelationship<?>> relationships;

    private Set<EProjectCycle> cycles;

    public GraphTransferDTO( final Set<ProjectVersionRef> roots, final Set<ProjectRelationship<?>> relationships, final Set<EProjectCycle> cycles )
    {
        this.roots = roots;
        this.relationships = relationships;
        this.cycles = cycles;
    }

    public Set<ProjectVersionRef> getRoots()
    {
        return roots;
    }

    public Set<ProjectRelationship<?>> getRelationships()
    {
        return relationships;
    }

    public Set<EProjectCycle> getCycles()
    {
        return cycles;
    }

    public void setRoots( final Set<ProjectVersionRef> roots )
    {
        this.roots = roots;
    }

    public void setRelationships( final Set<ProjectRelationship<?>> relationships )
    {
        this.relationships = relationships;
    }

    public void setCycles( final Set<EProjectCycle> cycles )
    {
        this.cycles = cycles;
    }

}
