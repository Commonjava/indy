/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.subsys.datafile;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;

public final class DataFile
{

    private final File file;

    private final DataFileEventManager events;

    DataFile( final File file, final DataFileEventManager events )
    {
        this.file = file;
        this.events = events;
    }

    public String[] list()
    {
        return file.list();
    }

    public DataFile getChild( final String named )
    {
        return new DataFile( new File( file, named ), events );
    }

    public String readString()
        throws IOException
    {
        final String content = FileUtils.readFileToString( file );

        events.accessed( file );
        return content;
    }

    public void delete( final ChangeSummary summary )
        throws IOException
    {
        if ( file.exists() )
        {
            FileUtils.forceDelete( file );
            events.deleted( file, summary );
        }
    }

    public DataFile getParent()
    {
        final File parent = file.getParentFile();
        return parent == null ? null : new DataFile( parent, events );
    }

    public boolean mkdirs()
    {
        if ( file.isDirectory() )
        {
            return true;
        }

        return file.mkdirs();
    }

    public boolean exists()
    {
        return file.exists();
    }

    public void writeString( final String content, final ChangeSummary summary )
        throws IOException
    {
        FileUtils.write( file, content );
        events.modified( file, summary );
    }

    public void writeString( final String content, final String encoding, final ChangeSummary summary )
        throws IOException
    {
        FileUtils.write( file, content, encoding );
        events.modified( file, summary );
    }

    public void writeLines( final List<String> lines, final String encoding, final ChangeSummary summary )
        throws IOException
    {
        FileUtils.writeLines( file, lines, encoding );
        events.modified( file, summary );
    }

    public void writeLines( final List<String> lines, final ChangeSummary summary )
        throws IOException
    {
        FileUtils.writeLines( file, lines );
        events.modified( file, summary );
    }

    public void appendString( final String content, final ChangeSummary summary )
        throws IOException
    {
        FileUtils.write( file, content, true );
        events.modified( file, summary );
    }

    public void appendString( final String content, final String encoding, final ChangeSummary summary )
        throws IOException
    {
        FileUtils.write( file, content, encoding, true );
        events.modified( file, summary );
    }

    public void appendLines( final List<String> lines, final String encoding, final ChangeSummary summary )
        throws IOException
    {
        FileUtils.writeLines( file, lines, encoding, true );
        events.modified( file, summary );
    }

    public void appendLines( final List<String> lines, final ChangeSummary summary )
        throws IOException
    {
        FileUtils.writeLines( file, lines, true );
        events.modified( file, summary );
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

    public void renameTo( final DataFile target, final ChangeSummary summary )
    {
        file.renameTo( target.file );
        events.modified( file, summary );
        events.modified( target.file, summary );
    }

    public List<String> readLines()
        throws IOException
    {
        final List<String> lines = FileUtils.readLines( file );

        events.accessed( file );
        return lines;
    }

    public String getPath()
    {
        return file.getPath();
    }

    public void touch()
    {
        events.accessed( file );
    }

    public DataFile[] listFiles( final FileFilter fileFilter )
    {
        if ( !file.exists())
        {
            return new DataFile[0];
        }

        final File[] files = file.listFiles( fileFilter );
        if ( files == null )
        {
            return new DataFile[0];
        }

        final DataFile[] ffiles = new DataFile[files.length];
        for ( int i = 0; i < files.length; i++ )
        {
            ffiles[i] = new DataFile( files[i], events );
        }

        return ffiles;
    }

    public String getName()
    {
        return file.getName();
    }

    public File getDetachedFile()
    {
        return file;
    }

}
