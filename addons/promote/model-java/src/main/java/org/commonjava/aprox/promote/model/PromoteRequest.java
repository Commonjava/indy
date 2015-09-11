package org.commonjava.aprox.promote.model;

import org.commonjava.aprox.model.core.StoreKey;

/**
 * Created by jdcasey on 9/11/15.
 */
public interface PromoteRequest<T extends PromoteRequest<T>>
{
    StoreKey getSource();

    T setSource( StoreKey source );

    StoreKey getTargetKey();
}
