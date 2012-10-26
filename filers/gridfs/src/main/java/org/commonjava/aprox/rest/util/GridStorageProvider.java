package org.commonjava.aprox.rest.util;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.filer.PathUtils;
import org.commonjava.aprox.io.StorageProvider;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.util.logging.Logger;
import org.infinispan.Cache;
import org.infinispan.io.GridFile;
import org.infinispan.io.GridFilesystem;
import org.infinispan.manager.CacheContainer;

@javax.enterprise.context.ApplicationScoped
public class GridStorageProvider
    implements StorageProvider
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private CacheContainer container;

    private Cache<String, byte[]> fsData;

    private Cache<String, GridFile.Metadata> fsMetadata;

    private GridFilesystem fs;

    public GridStorageProvider()
    {
    }

    public GridStorageProvider( final Cache<String, GridFile.Metadata> metadataCache,
                                final Cache<String, byte[]> dataCache )
    {
        this.fsData = dataCache;
        this.fsMetadata = metadataCache;
        start();
    }

    @PostConstruct
    public void init()
    {
        fsData = container.getCache( "aprox-storage-data" );
        fsData.start();

        fsMetadata = container.getCache( "aprox-storage-metadata" );
        fsMetadata.start();

        start();
    }

    public void start()
    {
        fs = new GridFilesystem( fsData, fsMetadata );
    }

    private String getPath( final StoreKey key, final String... parts )
    {
        final String name = "/" + key.getType()
                                     .name() + "-" + key.getName();

        return PathUtils.join( name, parts );
    }

    private String parentPath( final StoreKey key, final String path )
    {
        final String parentSub = parentPath( path );
        if ( parentSub == null )
        {
            return getPath( key, "" );
        }
        else
        {
            return getPath( key, parentSub );
        }
    }

    private String parentPath( final String path )
    {
        final String[] parts = path.split( "/" );
        if ( parts.length < 2 )
        {
            return null;
        }
        else
        {
            final String[] parentParts = new String[parts.length - 1];
            System.arraycopy( parts, 0, parentParts, 0, parentParts.length );
            return join( parentParts, "/" );
        }
    }

    @Override
    public boolean exists( final StoreKey key, final String path )
    {
        return fs.getFile( getPath( key, path ) )
                 .exists();
    }

    @Override
    public boolean isDirectory( final StoreKey key, final String path )
    {
        return fs.getFile( getPath( key, path ) )
                 .isDirectory();
    }

    @Override
    public InputStream openInputStream( final StoreKey key, final String path )
        throws IOException
    {
        final String parent = parentPath( key, path );
        logger.info( "Checking dir: %s", parent );

        final File dir = fs.getFile( parent );
        if ( !dir.isDirectory() )
        {
            throw new FileNotFoundException( "Parent directory: " + dir.getPath() + " does not exist." );
        }

        return fs.getInput( getPath( key, path ) );
    }

    @Override
    public OutputStream openOutputStream( final StoreKey key, final String path )
        throws IOException
    {
        final String parent = parentPath( key, path );
        logger.info( "Checking/creating dir: %s", parent );

        final File dir = fs.getFile( parent );
        if ( !dir.isDirectory() && !dir.mkdirs() )
        {
            throw new IOException( "Cannot create output directory: " + dir.getPath() );
        }

        return fs.getOutput( getPath( key, path ) );
    }

    // TODO: Could make this an alias system, with a count-down on alias removal to clean up orphaned files...
    @Override
    public void copy( final StoreKey fromKey, final String fromPath, final StoreKey toKey, final String toPath )
        throws IOException
    {
        if ( !exists( fromKey, fromPath ) )
        {
            throw new IOException( "Input file does not exist [key: " + fromKey + ", path: " + fromPath + "]" );
        }

        final File outfile = fs.getFile( parentPath( toKey, toPath ) );
        if ( !outfile.isDirectory() && !outfile.mkdirs() )
        {
            throw new IOException( "Cannot create output directory: " + outfile.getPath() );
        }

        InputStream in = null;
        OutputStream out = null;

        try
        {
            in = fs.getInput( getPath( fromKey, fromPath ) );
            out = fs.getOutput( getPath( toKey, toPath ) );
            IOUtils.copy( in, out );
        }
        finally
        {
            closeQuietly( in );
            closeQuietly( out );
        }
    }

    @Override
    public String getFilePath( final StoreKey key, final String path )
    {
        return getPath( key, path );
    }

    @Override
    public boolean delete( final StoreKey key, final String path )
        throws IOException
    {
        final File f = fs.getFile( getPath( key, path ) );
        if ( f.exists() )
        {
            return f.delete();
        }

        return false;
    }

    @Override
    public String[] list( final StoreKey key, final String path )
    {
        final File f = fs.getFile( getPath( key, path ) );
        if ( f.exists() )
        {
            return f.list();
        }

        return new String[] {};
    }

    @Override
    public File getDetachedFile( final StoreKey key, final String path )
    {
        return fs.getFile( getPath( key, path ) );
    }

    @Override
    public void mkdirs( final StoreKey key, final String path )
    {
        final File f = fs.getFile( getPath( key, path ) );
        f.mkdirs();
    }

}
