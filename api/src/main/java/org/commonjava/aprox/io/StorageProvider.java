package org.commonjava.aprox.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.commonjava.aprox.model.StoreKey;

public interface StorageProvider
{

    boolean exists( StoreKey key, String path );

    boolean isDirectory( StoreKey key, String path );

    InputStream openInputStream( StoreKey key, String path )
        throws IOException;

    OutputStream openOutputStream( StoreKey key, String path )
        throws IOException;

    void copy( StoreKey fromKey, String fromPath, StoreKey toKey, String toPath )
        throws IOException;

    String getFilePath( StoreKey key, String path );

    boolean delete( StoreKey key, String path )
        throws IOException;

    String[] list( StoreKey key, String path );

    File getDetachedFile( StoreKey key, String path );

    void mkdirs( StoreKey key, String path )
        throws IOException;

    void createFile( StoreKey key, String path )
        throws IOException;

}
