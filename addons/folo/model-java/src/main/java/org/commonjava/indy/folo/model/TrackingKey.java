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
package org.commonjava.indy.folo.model;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class TrackingKey implements Externalizable
{

    private String id;

    public TrackingKey()
    {
    }

    protected void setId( final String id )
    {
        if ( id == null )
        {
            throw new NullPointerException( "tracking id cannot be null." );
        }

        this.id = id;
    }

    public TrackingKey( final String id )
    {
        setId( id );
    }

    public String getId()
    {
        return id;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
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
        final TrackingKey other = (TrackingKey) obj;
        if ( id == null )
        {
            if ( other.id != null )
            {
                return false;
            }
        }
        else if ( !id.equals( other.id ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "TrackingKey [%s]", id );
    }

    @Override
    public void writeExternal( final ObjectOutput out )
            throws IOException
    {
        out.writeObject( id );
    }

    @Override
    public void readExternal( final ObjectInput in )
            throws IOException, ClassNotFoundException
    {
        id = (String) in.readObject();
    }
}
