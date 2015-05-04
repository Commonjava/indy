/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
