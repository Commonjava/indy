package org.commonjava.aprox.rest.util;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.io.StorageProvider;
import org.commonjava.aprox.model.StoreKey;
import org.infinispan.Cache;
import org.infinispan.io.GridFile;
import org.infinispan.io.GridFilesystem;

@Singleton
public class GridStorageProvider
    implements StorageProvider
{

    @Inject
    @AproxData
    @Named( "aprox-gridFs-data" )
    private Cache<String, byte[]> fsData;

    @Inject
    @AproxData
    @Named( "aprox-gridFs-metadata" )
    private Cache<String, GridFile.Metadata> fsMetadata;

    private GridFilesystem fs;

    @PostConstruct
    public void start()
    {
        fs = new GridFilesystem( fsData, fsMetadata );
    }

    private String getPath( final StoreKey key, final String... parts )
    {
        final String name = "/" + key.getType()
                                     .name() + "-" + key.getName();

        if ( parts.length < 1 )
        {
            return name;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append( name );
        for ( final String part : parts )
        {
            sb.append( "/" )
              .append( part.replace( '\\', '/' ) );
        }

        return sb.toString();
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
        return fs.getInput( getPath( key, path ) );
    }

    @Override
    public OutputStream openOutputStream( final StoreKey key, final String path )
        throws IOException
    {
        return fs.getOutput( getPath( key, path ) );
    }

    @Override
    public void copy( final StoreKey fromKey, final String fromPath, final StoreKey toKey, final String toPath )
        throws IOException
    {
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
    public void delete( final StoreKey key, final String path )
        throws IOException
    {
        fs.getFile( getPath( key, path ) )
          .delete();
    }

    @Override
    public String[] list( final StoreKey key, final String path )
    {
        return fs.getFile( getPath( key, path ) )
                 .list();
    }

    @Override
    public File getDetachedFile( final StoreKey key, final String path )
    {
        return fs.getFile( getPath( key, path ) );
    }

}
