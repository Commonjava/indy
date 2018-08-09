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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by jdcasey on 5/2/16.
 */
@ApplicationScoped
@Default
public class DefaultContentIndexManager
        implements ContentIndexManager, BootupAction, ShutdownAction
{
    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private SpecialPathManager specialPathManager;

    @ContentIndexCache
    @Inject
    private CacheHandle<IndexedStorePath, StoreKey> contentIndex;

    @Inject
    private NotFoundCache nfc;

    @Inject
    private NFCContentListener listener;

    @Inject
    private Instance<PackageIndexingStrategy> indexingStrategyComponents;

    private Map<String, PackageIndexingStrategy> indexingStrategies;

    protected DefaultContentIndexManager()
    {
    }

    public DefaultContentIndexManager( StoreDataManager storeDataManager, SpecialPathManager specialPathManager,
                                CacheHandle<IndexedStorePath, StoreKey> contentIndex,
                                Map<String, PackageIndexingStrategy> indexingStrategies,
                                NotFoundCache nfc )
    {
        this.storeDataManager = storeDataManager;
        this.specialPathManager = specialPathManager;
        this.contentIndex = contentIndex;
        this.indexingStrategies = indexingStrategies;
        this.nfc = nfc;
    }

    @PostConstruct
    public void constructed()
    {
        if ( indexingStrategyComponents != null )
        {
            Map<String, PackageIndexingStrategy> strats = new HashMap<>();
            indexingStrategyComponents.forEach( comp->{
                strats.put( comp.getPackageType(), comp );
            } );

            this.indexingStrategies = Collections.unmodifiableMap( strats );
        }
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
        //FIXME: Currently the content-index is GAV/Directory level, but NFC is not. That makes this NFC listener not usable
        //       as it is clearing the directory resource from NFC but the real ones in NFC are file resource. Need to think
        //       about implement NFC from GAV/Directory level too if we want to make this usable.
//        logger.debug( "Register index cache listener for NFC" );
//        contentIndex.execute( cache -> {
//            cache.addListener( listener );
//            return null;
//        } );
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

    @Override
    public boolean removeIndexedStorePath( String rawPath, StoreKey key, Consumer<IndexedStorePath> pathConsumer )
    {
        String path = getStrategyPath( key, rawPath );
        IndexedStorePath topPath = new IndexedStorePath( key, path );
        logger.trace( "Attempting to remove indexed path: {}", topPath );
        if ( contentIndex.remove( topPath ) != null )
        {
            if ( pathConsumer != null )
            {
                pathConsumer.accept( topPath );
            }
            return true;
        }

        logger.trace( "Remove index (NOT FOUND), key: {}", topPath );
        return false;
    }

    public String getStrategyPath( final StoreKey key, final String rawPath )
    {
        PackageIndexingStrategy strategy = indexingStrategies.get( key.getPackageType() );
        if ( strategy == null )
        {
            logger.trace( "Cannot find indexing strategy for package-type: {}. Using raw path for indexing.",
                          key.getPackageType() );

            return rawPath;
        }

        return strategy.getIndexPath( rawPath );
    }

    @Override
    public void deIndexStorePath( final StoreKey key, final String rawPath )
    {
        String path = getStrategyPath( key, rawPath );
        IndexedStorePath toRemove = new IndexedStorePath( key, path );
        StoreKey val = contentIndex.remove( toRemove );
        logger.trace( "De index{}, key: {}", ( val == null ? " (NOT FOUND)" : "" ), toRemove );
    }

    @Override
    public StoreKey getIndexedStoreKey( final StoreKey key, final String rawPath )
    {
        String path = getStrategyPath( key, rawPath );
        IndexedStorePath ispKey = new IndexedStorePath( key, path );
        StoreKey val = contentIndex.get( ispKey );
        logger.trace( "Get index{}, key: {}", ( val == null ? " (NOT FOUND)" : "" ), ispKey );
        return val;
    }

    @Override
    public void indexTransferIn( Transfer transfer, StoreKey...topKeys )
    {
        if ( transfer != null && transfer.exists() )
        {
            StoreKey key = LocationUtils.getKey( transfer );
            String path = getStrategyPath( key, transfer.getPath() );
            indexPathInStores( path, key, topKeys );
        }
    }

    /**
     * When we store or retrieve content, index it for faster reference next time.
     */
    @Override
    public void indexPathInStores( String rawPath, StoreKey originKey, StoreKey... topKeys )
    {
        String path = getStrategyPath( originKey, rawPath );

        IndexedStorePath origin = new IndexedStorePath( originKey, path );
        logger.trace( "Indexing path: {} in: {}", path, originKey );
        contentIndex.put( origin, originKey );

        Set<StoreKey> keySet = new HashSet<>( Arrays.asList( topKeys ) );
        keySet.forEach( ( key ) -> {
            IndexedStorePath isp = new IndexedStorePath( key, originKey, path );
            logger.trace( "Indexing path: {} in: {} via member: {}", path, key, originKey );
            contentIndex.put( isp, originKey );
        } );
    }

    @Override
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

    @Override
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

    @Override
    public void clearAllIndexedPathInStoreWithOriginal( ArtifactStore store, ArtifactStore originalStore )
    {
        Set<IndexedStorePath> isps = contentIndex.cacheKeySetByFilter(
                key -> key.getStoreKey().equals( store.getKey() ) && key.getOriginStoreKey()
                                                                        .equals( originalStore.getKey() ) );
        if ( isps != null )
        {
            isps.forEach( isp -> contentIndex.remove( isp ) );
        }
    }

    /**
     * <b>NOT Recursive</b>. This assumes you've recursed the group membership structure beforehand, using
     * {@link StoreDataManager#query()#getGroupsAffectedBy(Collection)} to find the set of {@link Group} instances for which
     * the path should be cleared.
     */
    @Override
    public void clearIndexedPathFrom( String rawPath, Set<Group> groups, Consumer<IndexedStorePath> pathConsumer )
    {
        if ( groups == null || groups.isEmpty() )
        {
            return;
        }

//        logger.debug( "Clearing path: '{}' from content index and storage of: {}", path, groups );

        groups.forEach( (group)->{
            String path = getStrategyPath( group.getKey(), rawPath );

            logger.debug( "Clearing path: '{}' from content index and storage of: {}", path, group.getName() );

            // if we remove an indexed path, it SHOULD mean there was content. If not, we should delete the NFC entry.
            if ( !removeIndexedStorePath( path, group.getKey(), pathConsumer ) )
            {
                ConcreteResource resource = new ConcreteResource( LocationUtils.toLocation( group ), path );
                nfc.clearMissing( resource );
            }
        } );
    }

}
