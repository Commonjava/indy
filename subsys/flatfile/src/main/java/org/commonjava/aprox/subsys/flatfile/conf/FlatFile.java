package org.commonjava.aprox.subsys.flatfile.conf;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

public final class FlatFile
{

    private final File file;

    private final FlatFileEventManager events;

    FlatFile( final File file, final FlatFileEventManager events )
    {
        this.file = file;
        this.events = events;
    }

    public String[] list()
    {
        return file.list();
    }

    public FlatFile getChild( final String named )
    {
        return new FlatFile( new File( file, named ), events );
    }

    public String readString()
        throws IOException
    {
        final String content = FileUtils.readFileToString( file );

        // TODO: events!
        return content;
    }

    public void delete()
        throws IOException
    {
        if ( file.exists() )
        {
            FileUtils.forceDelete( file );
        }

        // TODO: events!
    }

    public FlatFile getParent()
    {
        final File parent = file.getParentFile();
        return parent == null ? null : new FlatFile( parent, events );
    }

    public boolean mkdirs()
    {
        if ( file.isDirectory() )
        {
            return true;
        }

        final boolean result = file.mkdirs();

        // TODO: events!

        return result;
    }

    public boolean exists()
    {
        return file.exists();
    }

    public void writeString( final String content, final String encoding )
        throws IOException
    {
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

    public void renameTo( final FlatFile target )
    {
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

    public FlatFile[] listFiles( final FileFilter fileFilter )
    {
        final File[] files = file.listFiles( fileFilter );
        if ( files == null )
        {
            return null;
        }

        final FlatFile[] ffiles = new FlatFile[files.length];
        for ( int i = 0; i < files.length; i++ )
        {
            ffiles[i] = new FlatFile( files[i], events );
        }

        return ffiles;
    }

    public String getName()
    {
        return file.getName();
    }
}
