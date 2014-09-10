/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.subsys.datafile.change;

import java.io.File;
import java.util.Date;

import org.commonjava.aprox.audit.ChangeSummary;

public final class DataFileEvent
    implements Comparable<DataFileEvent>
{

    private static long COUNTER = 0;

    private final long id = COUNTER++;

    private final Date timestamp = new Date();

    private final File file;

    private final ChangeSummary summary;

    private final DataFileEventType type;

    DataFileEvent( final File file, final DataFileEventType type, final ChangeSummary summary )
    {
        this.file = file;
        this.type = type;
        this.summary = summary;
    }

    DataFileEvent( final File file )
    {
        this.file = file;
        this.type = DataFileEventType.accessed;
        this.summary = null;
    }

    @Override
    public String toString()
    {
        return String.format( "DataFileEvent [id=%s, file=%s, type=%s, timestamp=%s]%s", id, file, type,
                              ( summary == null ? ""
                        : "\nSummary: "
            + summary ) );
    }

    public File getFile()
    {
        return file;
    }

    public ChangeSummary getSummary()
    {
        return summary;
    }

    public DataFileEventType getType()
    {
        return type;
    }

    public long getId()
    {
        return id;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) ( id ^ ( id >>> 32 ) );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final DataFileEvent other = (DataFileEvent) obj;
        if ( id != other.id )
        {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo( final DataFileEvent other )
    {
        return Long.valueOf( id )
                   .compareTo( other.id );
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

}
