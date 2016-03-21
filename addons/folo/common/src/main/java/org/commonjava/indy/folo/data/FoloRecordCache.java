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
package org.commonjava.indy.folo.data;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.indy.folo.conf.FoloConfig;
import org.commonjava.indy.folo.data.idxmodel.AffectedStoreRecordKey;
import org.commonjava.indy.folo.model.AffectedStoreRecord;
import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackedContentRecord;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.hibernate.search.query.dsl.EntityContext;
import org.infinispan.Cache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.query.Search;
import org.infinispan.query.SearchManager;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class FoloRecordCache
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @ConfigureCache("folo-in-progress")
    @Inject
    private Cache<AffectedStoreRecordKey, AffectedStoreRecordKey> inProgressRecordCache;

    @ConfigureCache("folo-sealed")
    @Inject
    private Cache<TrackingKey, TrackedContentRecord> sealedRecordCache;

    protected FoloRecordCache()
    {
    }

    public FoloRecordCache( Cache<AffectedStoreRecordKey, AffectedStoreRecordKey> inProgressRecordCache, Cache<TrackingKey, TrackedContentRecord> sealedRecordCache )
    {
        this.inProgressRecordCache = inProgressRecordCache;
        this.sealedRecordCache = sealedRecordCache;
    }

    /**
     * Add a new artifact upload/download item to given affected store within a tracked-content record. If the tracked-content record doesn't exist,
     * or doesn't contain the specified affected store, values will be created on-demand.
     * @param key The key to the tracking record
     * @param affectedStore The store where the artifact was downloaded via / uploaded to
     * @param path The artifact's file path in the repo
     * @param effect Whether this is an upload or download event
     * @return True if a new record was stored, otherwise false
     */
    public synchronized boolean recordArtifact( final TrackingKey key, final StoreKey affectedStore, final String path,
                                                final StoreEffect effect )
        throws FoloContentException
    {
        if ( sealedRecordCache.containsKey( key ) )
        {
            throw new FoloContentException( "Tracking record: {} is already sealed!" );
        }

        AffectedStoreRecordKey ark = new AffectedStoreRecordKey( key, affectedStore, path, effect );
        if ( !inProgressRecordCache.containsKey( ark ) )
        {
            inProgressRecordCache.put( ark, ark );
            return true;
        }

        return false;
    }

    public synchronized void delete( final TrackingKey key )
    {
        sealedRecordCache.remove( key );
        inProgressByTrackingKey( key ).build().list().forEach( ( ark ) -> inProgressRecordCache.remove( ark ) );
    }

    public synchronized boolean hasRecord( final TrackingKey key )
    {
        return hasSealedRecord( key ) || hasInProgressRecord( key );
    }

    public synchronized boolean hasSealedRecord( final TrackingKey key )
    {
        return sealedRecordCache.containsKey( key );
    }

    public synchronized boolean hasInProgressRecord( final TrackingKey key )
    {
        return !sealedRecordCache.containsKey( key ) && inProgressByTrackingKey( key ).build().getResultSize() > 0;
    }

    public synchronized TrackedContentRecord get( final TrackingKey key )
    {
        return sealedRecordCache.get( key );
    }

    public TrackedContentRecord seal( TrackingKey key )
    {
        TrackedContentRecord record = sealedRecordCache.get( key );

        if ( record != null )
        {
            return record;
        }

        TrackedContentRecord created = new TrackedContentRecord( key );

        Query query = inProgressByTrackingKey( key ).build();
        List<AffectedStoreRecordKey> results = query.list();
        if ( results != null )
        {
            results.forEach( (result)->{
                AffectedStoreRecord store = created.getAffectedStore( result.getStoreKey(), true );
                store.add( result.getPath(), result.getEffect() );
                inProgressRecordCache.remove( result );
            });
        }

        sealedRecordCache.put( key, created );
        return created;
    }

    private QueryBuilder inProgressByTrackingKey( TrackingKey key )
    {
        QueryFactory queryFactory = Search.getQueryFactory( inProgressRecordCache );
        return queryFactory.from( AffectedStoreRecordKey.class )
                           .having( "key.id" )
                           .eq( key.getId() )
                           .toBuilder()
                           .orderBy( "index" );
    }

}
