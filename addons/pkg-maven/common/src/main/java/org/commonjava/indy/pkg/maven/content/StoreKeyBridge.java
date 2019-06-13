package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.model.core.StoreKey;
import org.hibernate.search.bridge.StringBridge;

public class StoreKeyBridge
                implements StringBridge
{
    @Override
    public String objectToString( Object o )
    {
        return ( (StoreKey) o ).toString();
    }
}
