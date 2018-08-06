/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.content.index;

import org.commonjava.indy.action.BootupAction;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.ShutdownAction;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by jdcasey on 5/2/16.
 */
@ApplicationScoped
public class ContentIndexManager
        implements BootupAction, ShutdownAction
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private SpecialPathManager specialPathManager;

    @ContentIndexCache
    @Inject
    private CacheHandle<IndexedStorePath, IndexedStorePath> contentIndex;

    @Inject
    private NotFoundCache nfc;

    @Inject
    private NFCContentListener listener;

    protected ContentIndexManager()
    {
    }

    public ContentIndexManager( StoreDataManager storeDataManager, SpecialPathManager specialPathManager,
                                CacheHandle<IndexedStorePath, IndexedStorePath> contentIndex,
                                NotFoundCache nfc )
    {
        this.storeDataManager = storeDataManager;
        this.specialPathManager = specialPathManager;
        this.contentIndex = contentIndex;
        this.nfc = nfc;
    }

    @Override
    public String getId()
    {
        return "Indy ContentIndexManager";
    }


    @Override
    public void init()
            throws IndyLifecycleException
    {
        logger.debug( "Register index cache listener for NFC" );
        contentIndex.execute( cache -> {
            cache.addListener( listener );
            return null;
        } );
    }

    @Override
    public void stop()
            throws IndyLifecycleException
    {
        logger.debug( "Shutdown index cache" );
        contentIndex.stop();
    }

    @Override
    public int getBootPriority()
    {
        return 80;
    }

    @Override
    public int getShutdownPriority()
    {
        return 95;
    }

    public boolean removeIndexedStorePath( String path, StoreKey key, Consumer<IndexedStorePath> pathConsumer )
    {
        IndexedStorePath topPath = new IndexedStorePath( key, path );
        if ( contentIndex.remove( topPath ) != null )
        {
            logger.trace( "Remove index, key: {}", topPath );
            if ( pathConsumer != null )
            {
                pathConsumer.accept( topPath );
            }
            return true;
        }

        logger.trace( "Remove index (NOT FOUND), key: {}", topPath );
        return false;
    }

    public void deIndexStorePath( final StoreKey key, final String path )
    {
        IndexedStorePath toRemove = new IndexedStorePath( key, path );
        IndexedStorePath val = contentIndex.remove( toRemove );
        logger.trace( "De index{}, key: {}", ( val == null ? " (NOT FOUND)" : "" ), toRemove );
    }

    public IndexedStorePath getIndexedStorePath( final StoreKey key, final String path )
    {
        IndexedStorePath ispKey = new IndexedStorePath( key, path );
        IndexedStorePath val = contentIndex.get( ispKey );
        logger.trace( "Get index{}, key: {}", ( val == null ? " (NOT FOUND)" : "" ), ispKey );
        return val;
    }

    public void indexTransferIn( Transfer transfer, StoreKey...topKeys )
    {
        if ( transfer != null && transfer.exists() )
        {
            indexPathInStores( transfer.getPath(), LocationUtils.getKey( transfer ), topKeys );
        }
    }

    /**
     * When we store or retrieve content, index it for faster reference next time.
     */
    public void indexPathInStores( String path, StoreKey originKey, StoreKey... topKeys )
    {
            IndexedStorePath origin = new IndexedStorePath( originKey, path );
            logger.trace( "Index, key: {}", origin );
            contentIndex.put( origin, origin );

            Set<StoreKey> keySet = new HashSet<>( Arrays.asList( topKeys ) );
            keySet.forEach( (key)->{
                IndexedStorePath isp = new IndexedStorePath( key, originKey, path );
                logger.trace( "Index, key: {}, origin: {}", isp, originKey );
                contentIndex.put( isp, origin );
            } );
    }

    public void clearAllIndexedPathInStore( ArtifactStore store )
    {
        Set<IndexedStorePath> isps =
                contentIndex.cacheKeySetByFilter( key -> key.getStoreKey().equals( store.getKey() ) );
        if ( isps != null )
        {
            isps.forEach( isp -> contentIndex.remove( isp ) );
        }
        logger.trace( "Clear all indices in: {}, size: {}", store.getKey(), ( isps != null ? isps.size() : 0 ) );
    }

    public void clearAllIndexedPathWithOriginalStore( ArtifactStore originalStore )
    {
        Set<IndexedStorePath> isps =
                contentIndex.cacheKeySetByFilter( key -> key.getOriginStoreKey().equals( originalStore.getKey() ) );
        if ( isps != null )
        {
            isps.forEach( isp -> contentIndex.remove( isp ) );
        }
        logger.trace( "Clear all indices with origin: {}, size: {}", originalStore.getKey(), ( isps != null ? isps.size() : 0 ) );
    }

    /**
     * <b>NOT Recursive</b>. This assumes you've recursed the group membership structure beforehand, using
     * {@link org.commonjava.indy.data.ArtifactStoreQuery#getGroupsAffectedBy(Collection)} to find the set of {@link Group} instances for which
     * the path should be cleared.
     */
    public void clearIndexedPathFrom( String path, Set<Group> groups, Consumer<IndexedStorePath> pathConsumer )
    {
        if ( groups == null || groups.isEmpty() )
        {
            return;
        }

        groups.forEach( (group)->{
            logger.trace( "Clearing path: '{}' from content index, group: {}", path, group.getName() );

            // if we remove an indexed path, it SHOULD mean there was content. If not, we should delete the NFC entry.
            if ( !removeIndexedStorePath( path, group.getKey(), pathConsumer ) )
            {
                ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( group ), path );
                nfc.clearMissing( resource );
            }
        } );
    }

}
