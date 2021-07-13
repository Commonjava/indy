/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.pathmapped.common;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.indy.pathmapped.model.PathMappedDeleteResult;
import org.commonjava.indy.pathmapped.model.PathMappedListResult;
import org.commonjava.maven.galley.cache.pathmapped.PathMappedCacheProvider;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.storage.pathmapped.core.PathMappedFileManager;
import org.commonjava.storage.pathmapped.spi.PathDB;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.InputStream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@ApplicationScoped
public class PathMappedController
{
    @Inject
    private CacheProvider cacheProvider;

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

    public PathMappedListResult list( String packageType, String type, String name, String path, boolean recursive,
                                      String fileType, int limit )
    {
        PathDB.FileType fType = PathDB.FileType.all;
        if ( isNotBlank( fileType ) )
        {
            fType = PathDB.FileType.valueOf( fileType );
        }
        String[] list;
        StoreKey storeKey = new StoreKey( packageType, StoreType.get( type ), name );
        if ( recursive )
        {
            int lmt = DEFAULT_RECURSIVE_LIST_LIMIT;
            if ( limit > 0 )
            {
                lmt = limit;
            }
            list = fileManager.list( storeKey.toString(), path, true, lmt, fType );
        }
        else
        {
            list = fileManager.list( storeKey.toString(), path, fType );
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
        // we just need a simple keyed location which provides the name to underlying pathMappedCacheProvider
        Location location = new SimpleKeyedLocation( storeKey );
        return new ConcreteResource( location, path );
    }

    private static class SimpleKeyedLocation
                    extends SimpleLocation
                    implements KeyedLocation
    {
        private final StoreKey storeKey;

        public SimpleKeyedLocation( StoreKey storeKey )
        {
            super( storeKey.toString() );
            this.storeKey = storeKey;
        }

        @Override
        public StoreKey getKey()
        {
            return storeKey;
        }
    }
}
