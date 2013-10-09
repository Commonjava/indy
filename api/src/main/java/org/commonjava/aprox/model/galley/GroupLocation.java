package org.commonjava.aprox.model.galley;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

public class GroupLocation
    extends CacheOnlyLocation
    implements KeyedLocation
{

    public GroupLocation( final String name )
    {
        super( new StoreKey( StoreType.group, name ) );
    }

    @Override
    public String toString()
    {
        return "GroupLocation [" + getKey() + "]";
    }
}
