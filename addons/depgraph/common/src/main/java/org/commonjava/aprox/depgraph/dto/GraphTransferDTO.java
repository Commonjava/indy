/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.depgraph.dto;

import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GraphTransferDTO
{

    private Set<ProjectVersionRef> gavs;

    private Set<ProjectRelationship<?>> relationships;

    private Set<EProjectCycle> cycles;

    public GraphTransferDTO( final Set<ProjectVersionRef> gavs, final Set<ProjectRelationship<?>> relationships,
                             final Set<EProjectCycle> cycles )
    {
        this.gavs = gavs;
        this.relationships = relationships;
        this.cycles = cycles;
    }

    public Set<ProjectVersionRef> getGAVs()
    {
        return gavs;
    }

    public Set<ProjectRelationship<?>> getRelationships()
    {
        return relationships;
    }

    public Set<EProjectCycle> getCycles()
    {
        return cycles;
    }

    public void setGAVs( final Set<ProjectVersionRef> roots )
    {
        this.gavs = roots;
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
