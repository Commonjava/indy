package org.commonjava.aprox.tensor.discover;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.EProjectRelationships;

final class RelationshipDiscoveryToken
{
    private final ProjectVersionRef ref;

    private EProjectRelationships relationships;

    private Throwable error;

    public RelationshipDiscoveryToken( final ProjectVersionRef ref )
    {
        this.ref = ref;
    }

    public synchronized void setRelationships( final EProjectRelationships relationships )
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

    public EProjectRelationships getRelationships()
    {
        return relationships;
    }

    public Throwable getError()
    {
        return error;
    }
}