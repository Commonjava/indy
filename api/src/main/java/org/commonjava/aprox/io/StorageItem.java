package org.commonjava.aprox.io;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.commonjava.aprox.change.event.FileAccessEvent;
import org.commonjava.aprox.change.event.FileDeletionEvent;
import org.commonjava.aprox.change.event.FileErrorEvent;
import org.commonjava.aprox.change.event.FileEventManager;
import org.commonjava.aprox.change.event.FileStorageEvent;
import org.commonjava.aprox.change.event.FileStorageEvent.Type;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

public final class StorageItem
{

    public static final String ROOT = "/";

    private static final String[] ROOT_ARRY = { ROOT };

    private final StoreKey key;

    private final String path;

    private final StorageProvider provider;

    private final FileEventManager fileEventManager;

    public StorageItem( final StoreKey key, final StorageProvider provider, final FileEventManager fileEventManager,
                        final String... path )
    {
        this.key = key;
        this.fileEventManager = fileEventManager;
        this.path = normalize( join( path, "/" ) );
        this.provider = provider;
    }

    private String normalize( final String path )
    {
        String result = path;
        while ( result.startsWith( "/" ) && result.length() > 1 )
        {
            result = result.substring( 1 );
        }

        return result;
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

        return new StorageItem( key, provider, fileEventManager, parentPath( path ) );
    }

    public StorageItem getChild( final String file )
    {
        return new StorageItem( key, provider, fileEventManager, path, file );
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

    public void touch()
    {
        fileEventManager.fire( new FileAccessEvent( this ) );
    }

    public InputStream openInputStream()
        throws IOException
    {
        return openInputStream( true );
    }

    public InputStream openInputStream( final boolean fireEvents )
        throws IOException
    {
        try
        {
            final InputStream stream = provider.openInputStream( key, path );
            if ( fireEvents )
            {
                fileEventManager.fire( new FileAccessEvent( this ) );
            }
            return stream;
        }
        catch ( final IOException e )
        {
            if ( fireEvents )
            {
                fileEventManager.fire( new FileErrorEvent( this, e ) );
            }
            throw e;
        }
    }

    public OutputStream openOutputStream()
        throws IOException
    {
        return openOutputStream( false, true );
    }

    public OutputStream openOutputStream( final boolean generated )
        throws IOException
    {
        return openOutputStream( false, true );
    }

    public OutputStream openOutputStream( final boolean generated, final boolean fireEvents )
        throws IOException
    {
        try
        {
            final OutputStream stream = provider.openOutputStream( key, path );

            if ( fireEvents )
            {
                Type type;
                if ( generated )
                {
                    type = Type.GENERATE;
                }
                else if ( key.getType() == StoreType.repository )
                {
                    type = Type.DOWNLOAD;
                }
                else
                {
                    type = Type.UPLOAD;
                }

                fileEventManager.fire( new FileStorageEvent( type, this ) );
            }

            return stream;
        }
        catch ( final IOException e )
        {
            if ( fireEvents )
            {
                fileEventManager.fire( new FileErrorEvent( this, e ) );
            }
            throw e;
        }
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

    public boolean delete()
        throws IOException
    {
        return delete( true );
    }

    public boolean delete( final boolean fireEvents )
        throws IOException
    {
        try
        {
            final boolean deleted = provider.delete( key, path );
            if ( deleted && fireEvents )
            {
                fileEventManager.fire( new FileDeletionEvent( this ) );
            }

            return deleted;
        }
        catch ( final IOException e )
        {
            if ( fireEvents )
            {
                fileEventManager.fire( new FileErrorEvent( this, e ) );
            }
            throw e;
        }
    }

    public String[] list()
    {
        return provider.list( key, path );
    }

    public File getDetachedFile()
    {
        return provider.getDetachedFile( key, path );
    }

    public void mkdirs()
        throws IOException
    {
        provider.mkdirs( key, path );
    }

    public void createFile()
        throws IOException
    {
        provider.createFile( key, path );
    }

}
