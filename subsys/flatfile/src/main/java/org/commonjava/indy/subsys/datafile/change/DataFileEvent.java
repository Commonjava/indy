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
package org.commonjava.indy.subsys.datafile.change;

import java.io.File;
import java.util.Date;

import org.commonjava.indy.audit.ChangeSummary;

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
        final String summ = summary == null ? "" : "\nSummary: " + summary;
        return String.format( "DataFileEvent [id=%s, file=%s, type=%s, timestamp=%s]%s", id, file, type, timestamp,
                              summ );
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
        return id == other.id;
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
