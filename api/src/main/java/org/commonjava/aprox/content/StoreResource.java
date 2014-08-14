package org.commonjava.aprox.content;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.maven.galley.model.ConcreteResource;

public class StoreResource
    extends ConcreteResource
{

    public StoreResource( final KeyedLocation location, final String... path )
    {
        super( location, path );
    }

    public StoreKey getStoreKey()
    {
        return ( (KeyedLocation) getLocation() ).getKey();
    }
}
