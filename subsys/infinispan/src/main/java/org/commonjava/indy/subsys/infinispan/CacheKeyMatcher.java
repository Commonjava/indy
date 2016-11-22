package org.commonjava.indy.subsys.infinispan;

import java.util.Set;

/**
 * Used to match the keys which are contained in {@link org.infinispan.Cache} wrapped in {@link CacheHandle}.
 *
 * @param <T> the type of the cache key
 */
@FunctionalInterface
public interface CacheKeyMatcher<T>
{
    Set<T> matches( CacheHandle<T, ?> cacheHandle );
}
