/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.apache.commons.codec.digest.DigestUtils;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by jdcasey on 3/15/16.
 */
@Indexed(index = "indexedStorePath")
public class IndexedStorePath
        implements Externalizable
{

    public static final String ORIGIN_STORE = "originStoreHash";

    public static final String STORE = "storeHash";

    public static final String PATH = "pathHash";

    @Field( name = STORE, store = Store.YES, analyze = Analyze.NO )
    private String storeHash;

    @Field( name = ORIGIN_STORE, store = Store.YES, analyze = Analyze.NO )
    private String originStoreHash;

    @Field( name = PATH, store = Store.YES, analyze = Analyze.NO )
    private String pathHash;

    public IndexedStorePath( StoreKey storeKey, String path )
    {
        this.storeHash = StoreKey.dedupe( storeKey ).getHashed();
        this.pathHash = DigestUtils.md5Hex( path );
    }

    public IndexedStorePath( StoreKey storeKey, StoreKey origin, String path )
    {
        this.storeHash = StoreKey.dedupe( storeKey ).getHashed();
        this.originStoreHash = StoreKey.dedupe( origin ).getHashed();
        this.pathHash = DigestUtils.md5Hex( path );
    }

    public String getStoreHash()
    {
        return storeHash;
    }

    public String getOriginStoreHash()
    {
        return originStoreHash;
    }

    public String getPathHash()
    {
        return pathHash;
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

        if ( !getStoreHash().equals( that.getStoreHash() ) )
        {
            return false;
        }
        return getPathHash().equals( that.getPathHash() );

    }

    @Override
    public int hashCode()
    {
        int result = getStoreHash().hashCode();
        result = 31 * result + getPathHash().hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "IndexedStorePath{" +
                "storeHash='" + storeHash + '\'' +
                ", originStoreHash='" + originStoreHash + '\'' +
                ", pathHash='" + pathHash + '\'' +
                '}';
    }

    @Override
    public void writeExternal( ObjectOutput out )
            throws IOException
    {
        out.writeObject( storeHash );
        out.writeObject( originStoreHash );
        out.writeObject( pathHash );
    }

    @Override
    public void readExternal( ObjectInput in )
            throws IOException, ClassNotFoundException
    {
        storeHash = (String) in.readObject();
        originStoreHash = (String) in.readObject();
        pathHash = (String) in.readObject();
    }
}
