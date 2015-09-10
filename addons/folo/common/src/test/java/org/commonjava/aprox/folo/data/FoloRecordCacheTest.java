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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.commonjava.aprox.folo.conf.FoloConfig;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEventManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class FoloRecordCacheTest
{

    private final class TestCache
        extends FoloRecordCache
    {
        private TestCache( final FoloFiler filer, final AproxObjectMapper objectMapper,
                           final FoloConfig config )
        {
            super( filer, objectMapper, config );
        }

        @Override
        public void write( final TrackedContentRecord record )
        {
            super.write( record );
        }

        public void setRecordCache( final Cache<TrackingKey, TrackedContentRecord> cache )
        {
            this.recordCache = cache;
        }
    }

    private FoloFiler filer;

    private DataFileManager dataFileManager;

    private AproxObjectMapper objectMapper;

    private TestCache cache;

    long ticker;

    boolean loadCalled = false;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setup()
        throws Exception
    {
        dataFileManager = new DataFileManager( temp.newFolder(), new DataFileEventManager() );
        objectMapper = new AproxObjectMapper( false );
        filer = new FoloFiler( dataFileManager );
        cache = new TestCache( filer, objectMapper, new FoloConfig( 0 ) );
        cache.buildCache();
    }

    @Test
    public void cacheCreatesNewRecordIfNoneExist()
        throws Exception
    {
        final Cache<TrackingKey, TrackedContentRecord> cache = CacheBuilder.newBuilder()
                                                                           .build();

        final TrackingKey key = newKey();
        final TrackedContentRecord record = cache.get( key, this.cache.newCallable( key ) );
        assertThat( record, notNullValue() );

        assertThat( cache.size(), equalTo( 1L ) );
    }

    @Test
    public void createCacheAddItemAndInvalidateAll_VerifyFileWritten()
    {
        final Cache<TrackingKey, TrackedContentRecord> cache = CacheBuilder.newBuilder()
                                                                           .removalListener( this.cache )
                                                                           .build();

        final TrackedContentRecord record = newRecord();
        cache.put( record.getKey(), record );

        assertThat( cache.size(), equalTo( 1L ) );

        cache.invalidateAll();

        assertThat( cache.size(), equalTo( 0L ) );

        final File f = getFile( record );
        System.out.println( "Checking: " + f );
        assertThat( f.exists(), equalTo( true ) );
    }

    private File getFile( TrackedContentRecord record )
    {
        return this.filer.getRecordFile( record.getKey() ).getDetachedFile();
    }

    @Test
    public void createCacheAddItem_InvalidateAll_VerifyFileWritten_ThenDeleteAndVerifyFileRemoved()
    {
        final Cache<TrackingKey, TrackedContentRecord> cache = CacheBuilder.newBuilder()
                                                                           .removalListener( this.cache )
                                                                           .build();

        final TrackedContentRecord record = newRecord();
        cache.put( record.getKey(), record );

        assertThat( cache.size(), equalTo( 1L ) );

        cache.invalidateAll();

        assertThat( cache.size(), equalTo( 0L ) );

        final File f = getFile( record );
        System.out.println( "Checking: " + f );
        assertThat( f.exists(), equalTo( true ) );

        this.cache.delete( record.getKey() );
        assertThat( f.exists(), equalTo( false ) );
    }

    @Test
    public void createCacheAddItemAndWaitForExpiration_VerifyFileWritten_RawCache()
        throws Exception
    {
        ticker = System.nanoTime();

        final Cache<TrackingKey, TrackedContentRecord> recordCache =
            CacheBuilder.newBuilder()
                                                                           .expireAfterAccess( 1, TimeUnit.MINUTES )
                                                                           .ticker( new Ticker()
                                                                           {
                                                                               @Override
                                                                               public long read()
                                                                               {
                                                                                   return ticker;
                                                                               }
                                                                           } )
                        .removalListener( this.cache )
                                                                           .build();

        final TrackedContentRecord record = newRecord();
        recordCache.put( record.getKey(), record );

        assertThat( recordCache.size(), equalTo( 1L ) );

        ticker += 60 * 1000000000L + 1;
        assertThat( recordCache.get( record.getKey(), new Callable<TrackedContentRecord>()
        {
            @Override
            public TrackedContentRecord call()
                throws Exception
            {
                loadCalled = true;
                return record;
            }
        } ), notNullValue() );

        assertThat( loadCalled, equalTo( true ) );

        final File f = getFile( record );
        System.out.println( "Checking: " + f );
        assertThat( f.exists(), equalTo( true ) );
    }

    @Test
    public void createCacheAddItemAndWaitForExpiration_VerifyFileWritten()
        throws Exception
    {
        ticker = System.nanoTime();

        final Cache<TrackingKey, TrackedContentRecord> recordCache =
            CacheBuilder.newBuilder()
                        .expireAfterAccess( 1, TimeUnit.MINUTES )
                        .ticker( new Ticker()
                        {
                            @Override
                            public long read()
                            {
                                return ticker;
                            }
                        } )
                        .removalListener( this.cache )
                        .build();

        this.cache.setRecordCache( recordCache );

        final TrackedContentRecord record = newRecord();
        recordCache.put( record.getKey(), record );

        assertThat( recordCache.size(), equalTo( 1L ) );

        ticker += 60 * 1000000000L + 1;
        assertThat( this.cache.get( record.getKey() ), notNullValue() );

        final File f = getFile( record );
        System.out.println( "Checking: " + f );
        assertThat( f.exists(), equalTo( true ) );
    }

    @Test
    public void writeRecordCreateCacheWithLoaderAndGetItem_VerifyRecordLoaded()
        throws Exception
    {
        final TrackedContentRecord record = newRecord();
        this.cache.write( record );

        final File f = getFile( record );
        System.out.println( "Checking: " + f );
        assertThat( f.exists(), equalTo( true ) );

        final Cache<TrackingKey, TrackedContentRecord> cache = CacheBuilder.newBuilder()
                                                                           .build();

        final TrackedContentRecord result = cache.get( record.getKey(), this.cache.newCallable( record.getKey() ) );

        assertThat( result, notNullValue() );
        assertThat( result.getKey(), equalTo( record.getKey() ) );
        assertThat( cache.size(), equalTo( 1L ) );

    }

    private TrackingKey newKey()
    {
        final String id = "track";

        return new TrackingKey( id );
    }

    private TrackedContentRecord newRecord()
    {
        return new TrackedContentRecord( newKey() );
    }

}
