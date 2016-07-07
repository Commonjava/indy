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

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.infinispan.Cache;
import org.infinispan.cdi.ConfigureCache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@ApplicationScoped
public class FoloRecordCache
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @ConfigureCache("folo-in-progress")
    @Inject
    private Cache<TrackedContentEntry, TrackedContentEntry> inProgressRecordCache;

    @ConfigureCache("folo-sealed")
    @Inject
    private Cache<TrackingKey, TrackedContent> sealedRecordCache;

    protected FoloRecordCache()
    {
    }

    public FoloRecordCache( Cache<TrackedContentEntry, TrackedContentEntry> inProgressRecordCache,
                            Cache<TrackingKey, TrackedContent> sealedRecordCache )
    {
        this.inProgressRecordCache = inProgressRecordCache;
        this.sealedRecordCache = sealedRecordCache;
    }

    /**
     * Add a new artifact upload/download item to given affected store within a tracked-content record. If the tracked-content record doesn't exist,
     * or doesn't contain the specified affected store, values will be created on-demand.
     * @param entry The TrackedContentEntry which will be cached
     * @return True if a new record was stored, otherwise false
     */
    public synchronized boolean recordArtifact( TrackedContentEntry entry )
            throws FoloContentException,IndyWorkflowException
    {
        if ( sealedRecordCache.containsKey( entry.getTrackingKey() ) )
        {
            throw new FoloContentException( "Tracking record: {} is already sealed!" );
        }

        if ( !inProgressRecordCache.containsKey( entry ) )
        {
            inProgressRecordCache.put( entry, entry );
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

    public synchronized TrackedContent get( final TrackingKey key )
    {
        return sealedRecordCache.get( key );
    }

    public TrackedContent seal( TrackingKey trackingKey )
    {
        TrackedContent record = sealedRecordCache.get( trackingKey );

        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( record != null )
        {
            logger.debug( "Tracking record: {} already sealed! Returning sealed record.", trackingKey );
            return record;
        }

        TrackedContent created = null;

        logger.debug( "Listing unsealed tracking record entries for: {}...", trackingKey );
        Query query = inProgressByTrackingKey( trackingKey ).build();
        List<TrackedContentEntry> results = query.list();
        if ( results != null )
        {
            logger.debug( "Adding {} entries to record: {}", results.size(), trackingKey );
            Set<TrackedContentEntry> uploads = new TreeSet<>(  );
            Set<TrackedContentEntry> downloads = new TreeSet<>(  );
            results.forEach( ( result ) -> {
                if ( StoreEffect.DOWNLOAD == result.getEffect() )
                {
                    downloads.add( result );
                }
                else if ( StoreEffect.UPLOAD == result.getEffect() )
                {
                    uploads.add( result );
                }
                logger.debug( "Removing in-progress entry: {}", result );
                inProgressRecordCache.remove( result );
            } );
            created = new TrackedContent( trackingKey, uploads, downloads );
        }

        logger.debug( "Sealing record for: {}", trackingKey );
        sealedRecordCache.put( trackingKey, created );
        return created;
    }

    private QueryBuilder inProgressByTrackingKey( TrackingKey key )
    {
        QueryFactory queryFactory = Search.getQueryFactory( inProgressRecordCache );
        return queryFactory.from( TrackedContentEntry.class )
                           .having( "trackingKey.id" )
                           .eq( key.getId() )
                           .toBuilder()
                           .orderBy( "index" );
    }

}
