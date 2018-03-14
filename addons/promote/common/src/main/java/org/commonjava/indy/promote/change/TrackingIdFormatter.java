package org.commonjava.indy.promote.change;

import org.commonjava.indy.model.core.StoreKey;

/**
 * Created by ruhan on 3/14/18.
 */
public interface TrackingIdFormatter
{
    String format( StoreKey storeKey );
}
