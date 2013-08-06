package org.commonjava.aprox.model.galley;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.maven.galley.model.Location;

public interface KeyedLocation
    extends Location
{

    StoreKey getKey();

}
