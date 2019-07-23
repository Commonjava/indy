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
package org.commonjava.indy.folo.data.idxmodel;

import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.StoreKey;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

/**
 * Created by jdcasey on 3/8/16.
 */
@Deprecated
@Indexed
public class AffectedStoreRecordKey
{
    @IndexedEmbedded
    private TrackingKey key;

    @Field
    private StoreKey storeKey;

    @Field
    private String path;

    @Field
    private StoreEffect effect;

    @Field
    private long index = System.currentTimeMillis();

    public AffectedStoreRecordKey( TrackingKey key, StoreKey storeKey, String path, StoreEffect effect )
    {
        this.key = key;
        this.storeKey = storeKey;
        this.path = path;
        this.effect = effect;
    }

    public long getIndex()
    {
        return index;
    }

    public TrackingKey getKey()
    {
        return key;
    }

    public StoreKey getStoreKey()
    {
        return storeKey;
    }

    public String getPath()
    {
        return path;
    }

    public StoreEffect getEffect()
    {
        return effect;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof AffectedStoreRecordKey ) )
        {
            return false;
        }

        AffectedStoreRecordKey that = (AffectedStoreRecordKey) o;

        if ( !getKey().equals( that.getKey() ) )
        {
            return false;
        }
        if ( !getStoreKey().equals( that.getStoreKey() ) )
        {
            return false;
        }
        if ( !getPath().equals( that.getPath() ) )
        {
            return false;
        }
        return getEffect() == that.getEffect();

    }

    @Override
    public int hashCode()
    {
        int result = getKey().hashCode();
        result = 31 * result + getStoreKey().hashCode();
        result = 31 * result + getPath().hashCode();
        result = 31 * result + getEffect().hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return "AffectedStoreRecordKey{" +
                "key=" + key +
                ", storeKey=" + storeKey +
                ", path='" + path + '\'' +
                ", effect=" + effect +
                ", index=" + index +
                '}';
    }
}
