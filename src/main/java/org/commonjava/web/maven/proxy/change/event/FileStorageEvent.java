package org.commonjava.web.maven.proxy.change.event;

import java.io.File;

public class FileStorageEvent
{

    public enum Type
    {
        DOWNLOAD, GENERATE, UPLOAD;
    }

    private final Type type;

    private final String repository;

    private final String path;

    private final File storageLocation;

    public FileStorageEvent( final Type type, final String repository, final String path,
                             final File storageLocation )
    {
        this.type = type;
        this.repository = repository;
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

    public File getStorageLocation()
    {
        return storageLocation;
    }

    public String getRepository()
    {
        return repository;
    }

}
