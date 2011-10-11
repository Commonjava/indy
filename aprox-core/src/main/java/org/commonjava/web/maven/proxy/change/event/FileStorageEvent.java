package org.commonjava.web.maven.proxy.change.event;

import java.io.File;

import org.commonjava.web.maven.proxy.model.ArtifactStore;

public class FileStorageEvent
{

    public enum Type
    {
        DOWNLOAD, GENERATE, UPLOAD;
    }

    private final Type type;

    private final ArtifactStore store;

    private final String path;

    private final String storageLocation;

    public FileStorageEvent( final Type type, final ArtifactStore store, final String path,
                             final File storageLocation )
    {
        this.type = type;
        this.store = store;
        this.path = path;
        this.storageLocation = storageLocation.getAbsolutePath();
    }

    public FileStorageEvent( final Type type, final ArtifactStore store, final String path,
                             final String storageLocation )
    {
        this.type = type;
        this.store = store;
        this.path = path;
        this.storageLocation = storageLocation;
    }

    public Type getType()
    {
        return type;
    }

    public String getPath()
    {
        return path;
    }

    public String getStorageLocation()
    {
        return storageLocation;
    }

    public ArtifactStore getStore()
    {
        return store;
    }

}
