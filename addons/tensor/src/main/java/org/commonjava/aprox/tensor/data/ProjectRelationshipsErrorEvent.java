package org.commonjava.aprox.tensor.data;


public class ProjectRelationshipsErrorEvent
{

    private final ErrorKey key;

    private final Throwable error;

    public ProjectRelationshipsErrorEvent( final ErrorKey key, final Throwable error )
    {
        this.key = key;
        this.error = error;
    }

    public ErrorKey getKey()
    {
        return key;
    }

    public Throwable getError()
    {
        return error;
    }

}
