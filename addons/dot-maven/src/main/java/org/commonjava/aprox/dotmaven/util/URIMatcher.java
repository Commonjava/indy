package org.commonjava.aprox.dotmaven.util;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;

public interface URIMatcher
{

    StoreType getStoreType();

    StoreKey getStoreKey();

    String getURI();

    boolean matches();

}