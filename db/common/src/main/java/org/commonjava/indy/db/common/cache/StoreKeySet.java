package org.commonjava.indy.db.common.cache;

import org.commonjava.indy.model.core.StoreKey;

import java.util.HashSet;
import java.util.Set;

public class StoreKeySet
{

    Set<StoreKey> storeKeys;

    public StoreKeySet () {}

    public StoreKeySet( Set<StoreKey> storeKeys )
    {
        this.storeKeys = storeKeys;
    }

    public Set<StoreKey> getStoreKeys() {
        if ( storeKeys == null )
        {
            storeKeys = new HashSet<>();
        }
        return storeKeys;
    }

    public void setStoreKeys(Set<StoreKey> storeKeys) {
        this.storeKeys = storeKeys;
    }

}
