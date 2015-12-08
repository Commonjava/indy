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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.commonjava.indy.folo.conf.FoloConfig;
import org.commonjava.indy.folo.model.AffectedStoreRecord;
import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackedContentRecord;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class FoloRecordCache
    extends CacheLoader<TrackingKey, TrackedContentRecord>
    implements RemovalListener<TrackingKey, TrackedContentRecord>
{

    private static final String JSON_TYPE = "json";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyObjectMapper objectMapper;

    @Inject
    private FoloConfig config;

    @Inject
    private FoloFiler filer;

    protected Cache<TrackingKey, TrackedContentRecord> recordCache;

    protected FoloRecordCache()
    {
    }

    public FoloRecordCache( final FoloFiler filer, final IndyObjectMapper objectMapper,
                            final FoloConfig config )
    {
        this.filer = filer;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    @PreDestroy
    public void shutdown()
    {
        // force eviction of all in memory to make sure they're written to disk.
        recordCache.invalidateAll();
    }

    @PostConstruct
    public void startCache()
    {
        buildCache();
    }

    protected Cache<TrackingKey, TrackedContentRecord> buildCache()
    {
        return buildCache( null );
    }

    protected Cache<TrackingKey, TrackedContentRecord> buildCache( FoloRecordCacheConfigurator builderConfigurator )
    {
        final CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder.removalListener( this );

        if ( builderConfigurator != null )
        {
            builderConfigurator.configure( builder );
        }
        else
        {
            builder.expireAfterAccess( config.getCacheTimeoutSeconds(), TimeUnit.SECONDS );
        }

        recordCache = builder.build( this );
        return recordCache;
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
    public synchronized TrackedContentRecord recordArtifact( final TrackingKey key, final StoreKey affectedStore, final String path,
                                                final StoreEffect effect )
            throws FoloContentException
    {
        final TrackedContentRecord record;
        try
        {
            record = recordCache.get( key, newCallable( key ) );
        }
        catch ( ExecutionException e )
        {
            throw new FoloContentException( "Failed to load tracking record for: %s. Reason: %s", e, key,
                                            e.getMessage() );
        }

        final AffectedStoreRecord affected = record.getAffectedStore( affectedStore, true );
        affected.add( path, effect );

        recordCache.put( record.getKey(), record );

        return record;
    }

    private Callable<? extends TrackedContentRecord> newCallable( TrackingKey key )
    {
        return ()->FoloRecordCache.this.load( key );
    }

    @Override
    public void onRemoval( final RemovalNotification<TrackingKey, TrackedContentRecord> notification )
    {
        final TrackingKey key = notification.getKey();
        if ( key == null )
        {
            logger.info( "Nothing to persist. Skipping." );
            return;
        }

        write( notification.getValue() );
    }

    protected void write( final TrackedContentRecord record )
    {
        final TrackingKey key = record.getKey();

        final File file = filer.getRecordFile( key ).getDetachedFile();
        logger.info( "Writing {} to: {}", key, file );
        try
        {
            file.getParentFile()
                .mkdirs();
            objectMapper.writeValue( file, record );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to persist folo log of artifact usage via: " + key, e );
        }
    }

    @Override
    public TrackedContentRecord load( final TrackingKey key )
        throws Exception
    {
        final DataFile file = filer.getRecordFile( key );
        if ( !file.exists() )
        {
            logger.info( "Creating new record for: {}", key );
            return new TrackedContentRecord( key );
        }

        logger.info( "Loading: {} from: {}", key, file );
        try
        {
            return objectMapper.readValue( file.getDetachedFile(), TrackedContentRecord.class );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read folo tracked record: " + key, e );
            throw new IllegalStateException( "Requested artimon tracked record: " + key
                + " is corrupt, and cannot be read.", e );
        }
    }

    public synchronized void delete( final TrackingKey key )
    {
        recordCache.invalidate( key );
        filer.deleteFiles( key );
    }

    public synchronized boolean hasRecord( final TrackingKey key )
    {
        DataFile rf = filer.getRecordFile( key );

        logger.info( "Looking for tracking record: {}.\nCache record: {}\nRecord file: {} (exists? {})", key,
                     recordCache.getIfPresent( key ), rf, rf.exists() );

        return recordCache.getIfPresent( key ) != null || rf.exists();
    }

    public synchronized TrackedContentRecord getOrCreate( final TrackingKey key )
        throws FoloContentException
    {
        try
        {
            return recordCache.get( key, ()-> FoloRecordCache.this.load( key ) );
        }
        catch ( final ExecutionException e )
        {
            throw new FoloContentException( "Failed to load tracking record for: %s. Reason: %s", e, key,
                                            e.getMessage() );
        }
    }

    public synchronized TrackedContentRecord getIfExists( TrackingKey key )
    {
        DataFile rf = filer.getRecordFile( key );

        TrackedContentRecord record = recordCache.getIfPresent( key );
        logger.info( "Looking for tracking record: {}.\nCache record: {}\nRecord file: {} (exists? {})", key,
                     record, rf, rf.exists() );

        if ( record == null && rf.exists() )
        {
            try
            {
                record = recordCache.get( key, () -> FoloRecordCache.this.load( key ) );
            }
            catch ( ExecutionException e )
            {
                logger.error(
                        String.format( "Failed to load Folo tracking record from: %s. Reason: %s", rf, e.getMessage() ),
                        e );

                record = null;
            }
        }

        return record;
    }

    /**
     * Mostly useful for testing, this allows fine-tuning of the configuration for the underlying Guava cache.
     */
    public interface FoloRecordCacheConfigurator
    {
        void configure(CacheBuilder<Object, Object> builder );
    }

}
