/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import org.commonjava.indy.IndyMetricsNames;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.metrics.IndyMetricsContentIndexNames;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.measure.annotation.IndyMetrics;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.measure.annotation.MetricNamed;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.subsys.infinispan.CacheHandle;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.io.SpecialPathManager;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.infinispan.cdi.ConfigureCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by jdcasey on 5/2/16.
 */
@ApplicationScoped
public class ContentIndexManager
{
    @Inject
    private StoreDataManager storeDataManager;

    @Inject
    private SpecialPathManager specialPathManager;

    @ConfigureCache( "content-index" )
    @ContentIndexCache
    @Inject
    private CacheHandle<IndexedStorePath, IndexedStorePath> contentIndex;

    @Inject
    private NotFoundCache nfc;

    protected ContentIndexManager(){}

    public ContentIndexManager( StoreDataManager storeDataManager, SpecialPathManager specialPathManager,
                                CacheHandle<IndexedStorePath, IndexedStorePath> contentIndex,
                                NotFoundCache nfc )
    {
        this.storeDataManager = storeDataManager;
        this.specialPathManager = specialPathManager;
        this.contentIndex = contentIndex;
        this.nfc = nfc;
    }

    @IndyMetrics( measure = @Measure( meters = @MetricNamed( name =
                    IndyMetricsContentIndexNames.METHOD_CONTENTINDEXMANAGER_REMOVEINDEXSTOREPATH
                                    + IndyMetricsNames.METER ), timers = @MetricNamed( name =
                    IndyMetricsContentIndexNames.METHOD_CONTENTINDEXMANAGER_REMOVEINDEXSTOREPATH
                                    + IndyMetricsNames.TIMER ) ) )
    public boolean removeIndexedStorePath( String path, StoreKey key, Consumer<IndexedStorePath> pathConsumer )
    {
//        Logger logger = LoggerFactory.getLogger( getClass() );
        IndexedStorePath topPath = new IndexedStorePath( key, path );
//        logger.trace( "Attempting to remove indexed path: {}", topPath );
        if ( contentIndex.remove( topPath ) != null )
        {
            if ( pathConsumer != null )
            {
                pathConsumer.accept( topPath );
            }
            return true;
        }

        return false;
    }

    @IndyMetrics( measure = @Measure( meters = @MetricNamed( name =
                    IndyMetricsContentIndexNames.METHOD_CONTENTINDEXMANAGER_DEINDEXSTOREPATH
                                    + IndyMetricsNames.METER ), timers = @MetricNamed( name =
                    IndyMetricsContentIndexNames.METHOD_CONTENTINDEXMANAGER_DEINDEXSTOREPATH
                                    + IndyMetricsNames.TIMER ) ) )
    public void deIndexStorePath( final StoreKey key, final String path )
    {
            IndexedStorePath toRemove = new IndexedStorePath( key, path );
            contentIndex.remove( toRemove );
    }

    public IndexedStorePath getIndexedStorePath( final StoreKey key, final String path )
            throws IndyWorkflowException
    {
        return contentIndex.get( new IndexedStorePath( key, path ) );
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
    @IndyMetrics( measure = @Measure( meters = @MetricNamed( name =
                    IndyMetricsContentIndexNames.METHOD_CONTENTINDEXMANAGER_INDEXPATHINSTORES
                                    + IndyMetricsNames.METER ), timers = @MetricNamed( name =
                    IndyMetricsContentIndexNames.METHOD_CONTENTINDEXMANAGER_INDEXPATHINSTORES
                                    + IndyMetricsNames.TIMER ) ) )
    public void indexPathInStores( String path, StoreKey originKey, StoreKey... topKeys )
    {
            IndexedStorePath origin = new IndexedStorePath( originKey, path );
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.trace( "Indexing path: {} in: {}", path, originKey );
            contentIndex.put( origin, origin );

            Set<StoreKey> keySet = new HashSet<>( Arrays.asList( topKeys ) );
            keySet.forEach( (key)->{
                IndexedStorePath isp = new IndexedStorePath( key, originKey, path );
                logger.trace( "Indexing path: {} in: {} via member: {}", path, key, originKey );
                contentIndex.put( isp, origin );
            } );
    }

    /**
     * <b>NOT Recursive</b>. This assumes you've recursed the group membership structure beforehand, using
     * {@link StoreDataManager#getGroupsAffectedBy(Collection)} to find the set of {@link Group} instances for which
     * the path should be cleared.
     */
    @IndyMetrics( measure = @Measure( meters = @MetricNamed( name =
                    IndyMetricsContentIndexNames.METHOD_CONTENTINDEXMANAGER_CLEARINDEXEDPATHFROM
                                    + IndyMetricsNames.METER ), timers = @MetricNamed( name =
                    IndyMetricsContentIndexNames.METHOD_CONTENTINDEXMANAGER_CLEARINDEXEDPATHFROM
                                    + IndyMetricsNames.TIMER ) ) )
    public void clearIndexedPathFrom( String path, Set<Group> groups, Consumer<IndexedStorePath> pathConsumer )
    {
        if ( groups == null || groups.isEmpty() )
        {
            return;
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
//        logger.debug( "Clearing path: '{}' from content index and storage of: {}", path, groups );

        groups.forEach( (group)->{
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
