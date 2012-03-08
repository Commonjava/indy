package org.commonjava.aprox.core.rest;

import java.io.FilterInputStream;
import java.io.InputStream;

import org.commonjava.aprox.core.model.StoreKey;

public class StoreInputStream
    extends FilterInputStream
{

    private final StoreKey key;

    private final String path;

    public StoreInputStream( final StoreKey key, final String path, final InputStream stream )
    {
        super( stream );
        this.key = key;
        this.path = path;
    }

    public String getPath()
    {
        return path;
    }

    public StoreKey getStoreKey()
    {
        return key;
    }

    @Override
    public String toString()
    {
        return key + "#" + path;
    }
}
