package org.commonjava.aprox.io;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.commonjava.aprox.model.StoreKey;

public final class StorageItem
{

    public static final String ROOT = "/";

    private static final String[] ROOT_ARRY = { ROOT };

    private final StoreKey key;

    private final String path;

    private final StorageProvider provider;

    public StorageItem( final StoreKey key, final StorageProvider provider, final String... path )
    {
        this.key = key;
        this.path = join( path, "/" );
        this.provider = provider;
    }

    public boolean isDirectory()
    {
        return provider.isDirectory( key, path );
    }

    public StoreKey getStoreKey()
    {
        return key;
    }

    public String getPath()
    {
        return path;
    }

    @Override
    public String toString()
    {
        return key + ":" + path;
    }

    public StorageItem getParent()
    {
        if ( path == ROOT || ROOT.equals( path ) )
        {
            return null;
        }

        return new StorageItem( key, provider, parentPath( path ) );
    }

    public StorageItem getChild( final String file )
    {
        return new StorageItem( key, provider, path, file );
    }

    private String[] parentPath( final String path )
    {
        final String[] parts = path.split( "/" );
        if ( parts.length == 1 )
        {
            return ROOT_ARRY;
        }
        else
        {
            final String[] parentParts = new String[parts.length - 1];
            System.arraycopy( parts, 0, parentParts, 0, parentParts.length );
            return parentParts;
        }
    }

    public InputStream openInputStream()
        throws IOException
    {
        return provider.openInputStream( key, path );
    }

    public OutputStream openOutputStream()
        throws IOException
    {
        return provider.openOutputStream( key, path );
    }

    public boolean exists()
    {
        return provider.exists( key, path );
    }

    public void copyFrom( final StorageItem f )
        throws IOException
    {
        provider.copy( f.getStoreKey(), f.getPath(), key, path );
    }

    public String getFullPath()
    {
        return provider.getFilePath( key, path );
    }

    public void delete()
        throws IOException
    {
        provider.delete( key, path );
    }

    public String[] list()
    {
        return provider.list( key, path );
    }

    public File getDetachedFile()
    {
        return provider.getDetachedFile( key, path );
    }

}
