package org.commonjava.indy.pathmapped.common;

import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pathmapped.model.PathMappedDeleteResult;
import org.commonjava.indy.pathmapped.model.PathMappedListResult;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.cache.pathmapped.PathMappedCacheProvider;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.storage.pathmapped.core.PathMappedFileManager;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.InputStream;

@ApplicationScoped
public class PathMappedController
{
    @Inject
    private CacheProvider cacheProvider;

    @Inject
    private StoreDataManager storeDataManager;

    private PathMappedCacheProvider pathMappedCacheProvider;

    private PathMappedFileManager fileManager;

    private static final int DEFAULT_RECURSIVE_LIST_LIMIT = 5000;

    public PathMappedController()
    {
    }

    @PostConstruct
    private void init()
    {
        if ( cacheProvider instanceof PathMappedCacheProvider )
        {
            pathMappedCacheProvider = (PathMappedCacheProvider) cacheProvider;
            fileManager = pathMappedCacheProvider.getPathMappedFileManager();
        }
    }

    public PathMappedDeleteResult delete( String packageType, String type, String name, String path ) throws Exception
    {
        ConcreteResource resource = getConcreteResource( packageType, type, name, path );
        boolean result = pathMappedCacheProvider.delete( resource );
        return new PathMappedDeleteResult( packageType, type, name, path, result );
    }

    public PathMappedListResult list( String packageType, String type, String name, String path, boolean recursive, int limit )
    {
        String[] list;
        StoreKey storeKey = new StoreKey( packageType, StoreType.get( type ), name );
        if ( recursive )
        {
            int lmt = DEFAULT_RECURSIVE_LIST_LIMIT;
            if ( limit > 0 )
            {
                lmt = limit;
            }
            list = fileManager.list( storeKey.toString(), path, true, lmt );
        }
        else
        {
            list = fileManager.list( storeKey.toString(), path );
        }
        return new PathMappedListResult( packageType, type, name, path, list );
    }

    public InputStream get( String packageType, String type, String name, String path ) throws Exception
    {
        ConcreteResource resource = getConcreteResource( packageType, type, name, path );
        return pathMappedCacheProvider.openInputStream( resource );
    }

    private ConcreteResource getConcreteResource( String packageType, String type, String name, String path )
                    throws Exception
    {
        StoreKey storeKey = new StoreKey( packageType, StoreType.get( type ), name );
        ArtifactStore store = storeDataManager.getArtifactStore( storeKey );
        return new ConcreteResource( LocationUtils.toLocation( store ), path );
    }

}
