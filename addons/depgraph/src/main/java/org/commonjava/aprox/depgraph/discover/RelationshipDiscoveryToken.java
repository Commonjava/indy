/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.discover;

import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

final class RelationshipDiscoveryToken
{
    private final ProjectVersionRef ref;

    private EProjectDirectRelationships relationships;

    private Throwable error;

    public RelationshipDiscoveryToken( final ProjectVersionRef ref )
    {
        this.ref = ref;
    }

    public synchronized void setRelationships( final EProjectDirectRelationships relationships )
    {
        this.relationships = relationships;
        notifyAll();
    }

    public synchronized void setError( final Throwable error )
    {
        this.error = error;
        notifyAll();
    }

    public ProjectVersionRef getRef()
    {
        return ref;
    }

    public EProjectDirectRelationships getRelationships()
    {
        return relationships;
    }

    public Throwable getError()
    {
        return error;
    }
}
