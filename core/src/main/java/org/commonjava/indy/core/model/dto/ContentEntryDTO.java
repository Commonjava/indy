/**
 * Copyright (C) 2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.model.dto;

import org.commonjava.indy.core.model.TrackingKey;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;

public class ContentEntryDTO
                implements Comparable<ContentEntryDTO>, Externalizable
{
    private static final int VERSION = 1;

    private StoreKey storeKey;

    private String path;

    public StoreKey getStoreKey()
    {
        return storeKey;
    }

    public void setStoreKey( StoreKey storeKey )
    {
        this.storeKey = storeKey;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    @Override
    public int compareTo( final ContentEntryDTO other )
    {
        int comp = storeKey.compareTo( other.getStoreKey() );
        if ( comp == 0 )
        {
            comp = path.compareTo( other.getPath() );
        }
        return comp;
    }

    @Override
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        out.writeObject( Integer.toString( VERSION ) );
        out.writeObject( storeKey.getPackageType() );
        out.writeObject( storeKey.getName() );
        out.writeObject( storeKey.getType().name() );
        out.writeObject( path == null ? "" : path );

    }

    @Override
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        Object whatIsThis = in.readObject();
        int version;
        String packageType;
        if ( whatIsThis == null )
        {
            // we see NumberFormatException: For input string: "null" in parseInt. It might be because we forget
            // to persist the trackingKey for some reason. In this case, we just ignore it and continues
            version = 1;
            packageType = PKG_TYPE_MAVEN;
        }
        else if ( whatIsThis instanceof TrackingKey )
        {
            version = 1;
            packageType = PKG_TYPE_MAVEN;
        }
        else
        {
            version = Integer.parseInt( String.valueOf( whatIsThis ) );
            packageType = (String) in.readObject();
        }
        // TODO: We should make future versioning / deserialization decisions based on the version we read / infer above
        if ( version > VERSION )
        {
            throw new IOException( "This class is of an older version: " + VERSION
                                                   + " vs. the version read from the data stream: " + version
                                                   + ". Cannot deserialize." );
        }
        final String storeKeyName = (String) in.readObject();
        final StoreType storeType = StoreType.get( (String) in.readObject() );
        storeKey = new StoreKey( packageType, storeType, storeKeyName );
        final String pathStr = (String) in.readObject();
        path = "".equals( pathStr ) ? null : pathStr;

    }
}
