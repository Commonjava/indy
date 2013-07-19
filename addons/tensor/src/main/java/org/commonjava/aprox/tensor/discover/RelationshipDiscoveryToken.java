package org.commonjava.aprox.tensor.discover;

import org.commonjava.maven.atlas.common.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.effective.EProjectDirectRelationships;

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