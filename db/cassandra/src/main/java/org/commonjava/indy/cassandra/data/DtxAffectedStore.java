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
