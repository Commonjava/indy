package org.commonjava.aprox.subsys.flatfile.conf;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.audit.SecuritySystem;

public final class DataFile
{

    private final File file;

    private final SecuritySystem security;

    private final DataFileEventManager events;

    DataFile( final File file, final DataFileEventManager events, final SecuritySystem security )
    {
        this.file = file;
        this.events = events;
        this.security = security;
    }

    public String[] list()
    {
        return file.list();
    }

    public DataFile getChild( final String named )
    {
        return new DataFile( new File( file, named ), events, security );
    }

    public String readString()
        throws IOException
    {
        final String content = FileUtils.readFileToString( file );

        // TODO: events!
        return content;
    }

    public void delete( final String summary )
        throws IOException
    {
        if ( file.exists() )
        {
            security.getCurrentPrincipal();

            FileUtils.forceDelete( file );
        }

        // TODO: events!
    }

    public DataFile getParent()
    {
        final File parent = file.getParentFile();
        return parent == null ? null : new DataFile( parent, events, security );
    }

    public boolean mkdirs()
    {
        if ( file.isDirectory() )
        {
            return true;
        }

        security.getCurrentPrincipal();

        final boolean result = file.mkdirs();

        // TODO: events!

        return result;
    }

    public boolean exists()
    {
        return file.exists();
    }

    public void writeString( final String content, final String encoding, final String summary )
        throws IOException
    {
        security.getCurrentPrincipal();

        FileUtils.write( file, content, encoding );
        // TODO: events!
    }

    @Override
    public String toString()
    {
        return file.getPath();
    }

    public boolean isDirectory()
    {
        return file.isDirectory();
    }

    public void renameTo( final DataFile target, final String summary )
    {
        security.getCurrentPrincipal();

        file.renameTo( target.file );
    }

    public List<String> readLines()
        throws IOException
    {
        final List<String> lines = FileUtils.readLines( file );

        // TODO: events!
        return lines;
    }

    public String getPath()
    {
        return file.getPath();
    }

    public DataFile[] listFiles( final FileFilter fileFilter )
    {
        final File[] files = file.listFiles( fileFilter );
        if ( files == null )
        {
            return null;
        }

        final DataFile[] ffiles = new DataFile[files.length];
        for ( int i = 0; i < files.length; i++ )
        {
            ffiles[i] = new DataFile( files[i], events, security );
        }

        return ffiles;
    }

    public String getName()
    {
        return file.getName();
    }
}
