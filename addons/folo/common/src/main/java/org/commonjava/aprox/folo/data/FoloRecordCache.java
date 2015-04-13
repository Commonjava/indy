package org.commonjava.aprox.folo.data;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.folo.conf.FoloConfig;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

@ApplicationScoped
public class FoloRecordCache
    extends CacheLoader<TrackingKey, TrackedContentRecord>
    implements RemovalListener<TrackingKey, TrackedContentRecord>
{

    private static final String DATA_DIR = "folo";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private DataFileManager dataFileManager;

    @Inject
    private AproxObjectMapper objectMapper;

    @Inject
    private FoloConfig config;

    protected Cache<TrackingKey, TrackedContentRecord> recordCache;

    protected FoloRecordCache()
    {
    }

    public FoloRecordCache( final DataFileManager dataFileManager, final AproxObjectMapper objectMapper,
                            final FoloConfig config )
    {
        this.dataFileManager = dataFileManager;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    @PostConstruct
    public void buildCache()
    {
        final CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();

        builder.expireAfterAccess( config.getCacheTimeoutSeconds(), TimeUnit.SECONDS )
               .removalListener( this );

        recordCache = builder.build( this );
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

        final File file = getFile( key );
        logger.info( "Writing {} to: {}", key, file );
        try
        {
            file.getParentFile()
                .mkdirs();
            objectMapper.writeValue( file, record );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to persist artimon log of artifact usage via: " + key, e );
        }
    }

    @Override
    public TrackedContentRecord load( final TrackingKey key )
        throws Exception
    {
        final File file = getFile( key );
        if ( !file.exists() )
        {
            logger.info( "Creating new record for: {}", key );
            return new TrackedContentRecord( key );
        }

        logger.info( "Loading: {} from: {}", key, file );
        try
        {
            return objectMapper.readValue( file, TrackedContentRecord.class );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read artimon tracked record: " + key, e );
            throw new IllegalStateException( "Requested artimon tracked record: " + key
                + " is corrupt, and cannot be read.", e );
        }
    }

    public void delete( final TrackingKey key )
    {
        recordCache.invalidate( key );
        final File file = getFile( key );
        if ( file.exists() )
        {
            logger.info( "Deleting: {} at: {}", key, file );
            file.delete();
        }
    }

    protected File getFile( final TrackingKey key )
    {
        final String fname = String.format( "%s.json", key.getId() );

        final DataFile dataFile = dataFileManager.getDataFile( DATA_DIR, fname );

        return dataFile.getDetachedFile();
    }

    public Callable<? extends TrackedContentRecord> newCallable( final TrackingKey trackedStore )
    {
        return new LoaderCall( this, trackedStore );
    }

    private static final class LoaderCall
        implements Callable<TrackedContentRecord>
    {
        private final FoloRecordCache persister;

        private final TrackingKey key;

        public LoaderCall( final FoloRecordCache persister, final TrackingKey key )
        {
            this.persister = persister;
            this.key = key;
        }

        @Override
        public TrackedContentRecord call()
            throws Exception
        {
            return persister.load( key );
        }
    }

    public boolean hasRecord( final TrackingKey key )
    {
        return recordCache.getIfPresent( key ) != null || getFile( key ).exists();
    }

    public TrackedContentRecord get( final TrackingKey key )
        throws FoloContentException
    {
        try
        {
            return recordCache.get( key, newCallable( key ) );
        }
        catch ( final ExecutionException e )
        {
            throw new FoloContentException( "Failed to load tracking record for: %s. Reason: %s", e, key,
                                            e.getMessage() );
        }
    }

}
