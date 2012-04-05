package org.commonjava.aprox.core.io;

import java.io.InputStream;

import org.commonjava.aprox.core.model.StoreKey;

public class StorageItem
{

    private final StoreKey key;

    private final String path;

    private final InputStream stream;

    public StorageItem( final StoreKey key, final String path, final InputStream stream )
    {
        this.key = key;
        this.path = path;
        this.stream = stream;
    }

    public StorageItem( final StoreKey key, final String path )
    {
        this.key = key;
        this.path = path;
        this.stream = null;
    }

    public boolean isDirectory()
    {
        return stream == null;
    }

    public StoreKey getStoreKey()
    {
        return key;
    }

    public String getPath()
    {
        return path;
    }

    public InputStream getStream()
    {
        return stream;
    }

    @Override
    public String toString()
    {
        return key + ":" + path;
    }

}
