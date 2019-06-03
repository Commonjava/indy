package org.commonjava.indy.pkg.maven.content;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;

import java.util.Set;
import java.util.stream.Collectors;

import static org.commonjava.indy.pkg.maven.content.group.MavenMetadataMerger.METADATA_NAME;
import static org.commonjava.maven.galley.util.PathUtils.normalize;
import static org.commonjava.maven.galley.util.PathUtils.parentPath;

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

    public static void remove( StoreKey storeKey, Set<String> paths,
                               CacheHandle<MetadataCacheKey, MetadataInfo> cacheHandle )
    {
        paths.forEach( p -> cacheHandle.remove( new MetadataCacheKey( storeKey, p ) ) );
    }

    public static void remove( StoreKey storeKey, String path, CacheHandle<MetadataCacheKey, MetadataInfo> cacheHandle )
    {
        cacheHandle.remove( new MetadataCacheKey( storeKey, path ) );
    }

    public static Set<String> getAllPaths( StoreKey storeKey, CacheHandle<MetadataCacheKey, MetadataInfo> cacheHandle )
    {
        return cacheHandle.executeCache( ( cache ) -> cache.keySet()
                                                           .stream()
                                                           .filter( ( k ) -> k.getStoreKey().equals( storeKey ) )
                                                           .map( ( k ) -> k.getPath() )
                                                           .collect( Collectors.toSet() ) );
    }

    public static String getMetadataPath( String pomPath )
    {
        final String versionPath = normalize( parentPath( pomPath ) );
        return normalize( normalize( parentPath( versionPath ) ), METADATA_NAME );
    }
}
