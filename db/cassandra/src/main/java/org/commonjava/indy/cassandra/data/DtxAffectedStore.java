/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.cassandra.data;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import org.commonjava.indy.model.core.StoreKey;

import java.util.HashSet;
import java.util.Set;

import static org.commonjava.indy.cassandra.data.CassandraStoreUtil.TABLE_AFFECTED_STORE;

@Table( name = TABLE_AFFECTED_STORE, readConsistency = "QUORUM", writeConsistency = "QUORUM" )
public class DtxAffectedStore
{

    @PartitionKey
    private String key;

    @Column
    private Set<String> affectedStores;

    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public Set<String> getAffectedStores()
    {
        return affectedStores;
    }

    public void setAffectedStores( Set<String> affectedStores )
    {
        this.affectedStores = affectedStores;
    }

    public Set<StoreKey> getAffectedStoreKeys()
    {
        Set<StoreKey> storeKeys = new HashSet<>();
        for ( String key : affectedStores )
        {
            storeKeys.add( StoreKey.fromString( key ) );
        }
        return storeKeys;
    }

}
