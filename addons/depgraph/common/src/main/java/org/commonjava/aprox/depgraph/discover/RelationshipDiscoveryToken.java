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
