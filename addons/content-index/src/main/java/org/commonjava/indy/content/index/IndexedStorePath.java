/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.content.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;

/**
 * Created by jdcasey on 3/15/16.
 */
public class IndexedStorePath
        implements Externalizable
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    private StoreType storeType;

    private String storeName;

    private StoreType originStoreType;

    private String originStoreName;

    private String path;

    private String packageType;

    // this needs to be public for Infinispan to not throw InvalidClassException with the first httprox request
    public IndexedStorePath(){}

    public IndexedStorePath( StoreKey storeKey, String path )
    {
        this.packageType = storeKey.getPackageType();
        if ( this.packageType == null )
        {
            this.packageType = MAVEN_PKG_KEY;
        }

        this.storeType = storeKey.getType();
        this.storeName = storeKey.getName();

        this.path = path;
    }

    public IndexedStorePath( StoreKey storeKey, StoreKey origin, String path )
    {
        this( storeKey, path );
        this.originStoreType = origin.getType();
        this.originStoreName = origin.getName();
    }

    @JsonIgnore
    public StoreKey getStoreKey()
    {
        return new StoreKey( packageType, storeType, storeName );
    }

    @JsonIgnore
    public StoreKey getOriginStoreKey()
    {
        return originStoreName == null ? null : new StoreKey( packageType, originStoreType, originStoreName );
    }

    public StoreType getStoreType()
    {
        return storeType;
    }

    public String getStoreName()
    {
        return storeName;
    }

    public String getPath()
    {
        return path;
    }

    public StoreType getOriginStoreType()
    {
        return originStoreType;
    }

    public String getOriginStoreName()
    {
        return originStoreName;
    }

    public String getPackageType()
    {
        return packageType;
    }

    @Override
    public String toString()
    {
        /* @formatter:off */
        return "IndexedStorePath{" +
                "packageType=" + packageType +
                ", storeType=" + storeType  +
                ", storeName='" + storeName + '\'' +
                ", originStoreType=" + originStoreType +
                ", originStoreName='" + originStoreName + '\'' +
                ", path='" + path + '\'' +
                '}';
        /* @formatter:on */
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof IndexedStorePath ) )
        {
            return false;
        }

        IndexedStorePath that = (IndexedStorePath) o;

        if ( !getPackageType().equals( that.getPackageType() ) )
        {
            return false;
        }
        if ( getStoreType() != that.getStoreType() )
        {
            return false;
        }
        if ( !getStoreName().equals( that.getStoreName() ) )
        {
            return false;
        }

        return getPath().equals( that.getPath() );

    }

    @Override
    public int hashCode()
    {
        int result = getPackageType().hashCode();
        result = 31 * result + getStoreType().hashCode();
        result = 31 * result + getStoreName().hashCode();
        result = 31 * result + getPath().hashCode();
        return result;
    }

    @Override
    public void writeExternal( ObjectOutput out )
            throws IOException
    {
        out.writeObject( storeType.name() );
        out.writeObject( storeName );

        if ( originStoreType != null )
        {
            out.writeObject( originStoreType.name() );
        }
        else
        {
            out.writeObject( "" );
        }
        if ( originStoreName != null )
        {
            out.writeObject( originStoreName );
        }
        else
        {
            out.writeObject( "" );
        }
        out.writeObject( path );
        out.writeObject( packageType );
    }

    @Override
    public void readExternal( ObjectInput in )
            throws IOException, ClassNotFoundException
    {
        storeType = StoreType.get( (String) in.readObject() );
        storeName = (String) in.readObject();
        final String osTypeString = (String) in.readObject();
        originStoreType = "".equals( osTypeString ) ? null : StoreType.get( osTypeString );
        final String osNameString = (String) in.readObject();
        originStoreName = "".equals( osNameString ) ? null : osNameString;
        path = (String) in.readObject();

        try
        {
            packageType = (String) in.readObject();
        }
        catch ( IOException e )
        {
            logger.warn( "Read packageType failed (probably reading an old data entry) and set to default 'maven', {}", e );
            packageType = MAVEN_PKG_KEY;
        }
    }
}
