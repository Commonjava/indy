package org.commonjava.indy.subsys.infinispan.metrics;

import java.util.Set;

/**
 * This is for other components to register ISPN caches so that they can be monitored by Metircs.
 */
public interface IspnCacheRegistry
{
    Set<String> getCacheNames();
}
