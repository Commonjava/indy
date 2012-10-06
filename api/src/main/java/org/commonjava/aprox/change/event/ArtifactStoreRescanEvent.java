package org.commonjava.aprox.change.event;

import org.commonjava.aprox.model.StoreKey;

public class ArtifactStoreRescanEvent
{

    private final StoreKey key;

    public ArtifactStoreRescanEvent( final StoreKey key )
    {
        this.key = key;
    }

    public StoreKey getStoreKey()
    {
        return key;
    }

}
