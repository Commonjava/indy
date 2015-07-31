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
package org.commonjava.aprox.folo.data;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.folo.model.AffectedStoreRecord;
import org.commonjava.aprox.folo.model.StoreEffect;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.StoreKey;

import com.google.common.base.Ticker;

/**
 * Manages recording artifact download/upload, and also retrieval of same.
 */
@ApplicationScoped
public class FoloRecordManager
{

    @Inject
    private FoloRecordCache cache;

    protected FoloRecordManager()
    {
    }

    /**
     * NOTE: This is for testing. You must call either {@link #init()} or {@link #cacheInit(Ticker)} before this object will be ready to use. 
     * @param cache
     * @param config
     */
    protected FoloRecordManager( final FoloRecordCache cache )
    {
        this.cache = cache;
    }

    /**
     * Add a new artifact upload/download item to given affected store within a tracked-content record. If the tracked-content record doesn't exist,
     * or doesn't contain the specified affected store, values will be created on-demand.
     * @param key The key to the tracking record
     * @param affectedStore The store where the artifact was downloaded via / uploaded to
     * @param path The artifact's file path in the repo
     * @param effect Whether this is an upload or download event
     * @return The changed record
     * @throws FoloContentException In case there is some problem loading an existing record from disk.
     */
    public TrackedContentRecord recordArtifact( final TrackingKey key, final StoreKey affectedStore, final String path,
                                final StoreEffect effect )
        throws FoloContentException
    {
        final TrackedContentRecord record = cache.get( key );
        final AffectedStoreRecord affected = record.getAffectedStore( affectedStore, true );
        affected.add( path, effect );

        return record;
    }

    /**
     * Retrieve the record of all content uploaded/downloaded for stores related to a given tracking key.
     * @param key The key to the tracking record
     * @return The record itself
     * @throws FoloContentException In case there is some problem loading an existing record from disk.
     */
    public TrackedContentRecord getRecord( final TrackingKey key )
        throws FoloContentException
    {
        if ( hasRecord( key ) )
        {
            return cache.get( key );
        }

        return null;
    }

    /**
     * If a record exists for the given key, invalidate it from the cache and delete the corresponding file on disk.
     * @param key The key to the tracking record
     */
    public void clearRecord( final TrackingKey key )
    {
        cache.delete( key );
    }

    /**
     * Check if the cache contains a record for this key, or the record has been persisted on disk.
     * @param key The key to the tracking record
     * @return true if the record exists
     */
    public boolean hasRecord( final TrackingKey key )
    {
        return cache.hasRecord( key );
    }

    public void initRecord( final TrackingKey key )
        throws FoloContentException
    {
        cache.get( key );
    }

}
