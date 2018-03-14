package org.commonjava.indy.promote.change

import org.commonjava.indy.model.core.StoreKey

class TrackingIdFormatterImpl implements TrackingIdFormatter {

    /**
     * Get the Folo tracking id which is used to adjust Folo record when user promotes temp build to a global hosted
     * repository (by path promotion).
     *
     * The default is the store name. While if the store name and tracking id is different, this provides a way to
     * format a store key to a tracking id.
     *
     * @param storeKey promotion source store key, e.g., build_ABC
     * @return Folo tracking id contains the source store
     */
    String format( StoreKey storeKey )
    {
        storeKey.getName()
    }
}