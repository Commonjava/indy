package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;

import java.util.Set;
import java.util.stream.Collectors;

public class MetadataUtil
{
    public static void removeAll( StoreKey storeKey, CacheHandle<MetadataCacheKey, MetadataInfo> cacheHandle )
    {
        cacheHandle.executeCache( ( cache ) -> {
            cache.keySet()
                 .stream()
                 .filter( ( k ) -> k.getStoreKey().equals( storeKey ) )
                 .forEach( ( k ) -> cache.remove( k ) );
            return null;
        } );
    }

    public static Set<String> getAllPaths( StoreKey storeKey, CacheHandle<MetadataCacheKey, MetadataInfo> cacheHandle )
    {
        return cacheHandle.executeCache( ( cache ) -> cache.keySet()
                                                           .stream()
                                                           .filter( ( k ) -> k.getStoreKey().equals( storeKey ) )
                                                           .map( ( k ) -> k.getPath() )
                                                           .collect( Collectors.toSet() ) );
    }
}
