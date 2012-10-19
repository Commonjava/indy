package org.commonjava.aprox.dotmaven.data;

import org.commonjava.aprox.model.ArtifactStore;

public class StorageAdvice
{
    private final boolean deployable;

    private final boolean releasesAllowed;

    private final boolean snapshotsAllowed;

    private final ArtifactStore store;

    public StorageAdvice( final ArtifactStore store, final boolean deployable, final boolean releasesAllowed,
                          final boolean snapshotsAllowed )
    {
        this.store = store;
        this.deployable = deployable;
        this.releasesAllowed = releasesAllowed;
        this.snapshotsAllowed = snapshotsAllowed;
    }

    public ArtifactStore getStore()
    {
        return store;
    }

    public boolean isDeployable()
    {
        return deployable;
    }

    public boolean isReleasesAllowed()
    {
        return releasesAllowed;
    }

    public boolean isSnapshotsAllowed()
    {
        return snapshotsAllowed;
    }

}