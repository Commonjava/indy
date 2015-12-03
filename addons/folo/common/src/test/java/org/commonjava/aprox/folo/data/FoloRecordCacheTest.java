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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.commonjava.aprox.folo.conf.FoloConfig;
import org.commonjava.aprox.folo.model.StoreEffect;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
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

    private interface WriteProcessor
    {
        void write( TestCache cache, TrackedContentRecord record );
    }

    private final class TestCache
            extends FoloRecordCache
    {
        private WriteProcessor writer = ( cache, record ) -> cache.doWrite( record );

        private TestCache( final FoloFiler filer, final AproxObjectMapper objectMapper, final FoloConfig config )
        {
            super( filer, objectMapper, config );
        }

        public void doWrite( final TrackedContentRecord record )
        {
            super.write( record );
        }

        @Override
        public void write( final TrackedContentRecord record )
        {
            writer.write( this, record );
        }

        @Override
        public Cache<TrackingKey, TrackedContentRecord> buildCache()
        {
            return super.buildCache();
        }

        @Override
        public Cache<TrackingKey, TrackedContentRecord> buildCache( FoloRecordCacheConfigurator builderConfigurator )
        {
            return super.buildCache( builderConfigurator );
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
        cache = new TestCache( filer, objectMapper, new FoloConfig( 2 ) );
    }

    @Test
    public void cacheCreatesNewRecordIfNoneExist()
            throws Exception
    {
        final Cache<TrackingKey, TrackedContentRecord> cache = this.cache.buildCache();

        final TrackingKey key = newKey();
        final TrackedContentRecord record = cache.get( key, ()->this.cache.load( key ) );
        assertThat( record, notNullValue() );

        assertThat( cache.size(), equalTo( 1L ) );
    }

    @Test
    public void addRecordThenLoadAfterTimedOut()
            throws Exception
    {
        final Cache<TrackingKey, TrackedContentRecord> cache = this.cache.buildCache();

        final TrackingKey key = newKey();
        TrackedContentRecord record = new TrackedContentRecord( key );
        cache.put( key, record );

        assertThat( cache.size(), equalTo( 1L ) );

        System.out.println( "Wait for timeout" );
        Thread.sleep( 2000 );

//        assertThat( cache.size(), equalTo( 0L ) );

        TrackedContentRecord retrieved = this.cache.getOrCreate( key );
        assertThat( retrieved, equalTo( record ) );
    }

    @Test
    public void createCacheAddItemAndInvalidateAll_VerifyFileWritten()
    {
        Cache<TrackingKey, TrackedContentRecord> cache = this.cache.buildCache();

        final TrackedContentRecord record = newRecord();
        cache.put( record.getKey(), record );

        assertThat( cache.size(), equalTo( 1L ) );

        cache.invalidateAll();

        assertThat( cache.size(), equalTo( 0L ) );

        final File f = getFile( record );
        System.out.println( "Checking: " + f );
        assertThat( f.exists(), equalTo( true ) );
    }

    @Test
    public void slowWrite_LoadWhileWriting()
            throws Exception
    {
        this.cache.writer = (cache, record)-> {
            try
            {
                System.out.println("Waiting 500ms before writing");
                Thread.sleep( 500 );
            }
            catch ( InterruptedException e )
            {
                fail( "Interrupted!" );
            }

            cache.doWrite( record );
            System.out.println("Write done");
        };

        Cache<TrackingKey, TrackedContentRecord> cache = this.cache.buildCache();

        final TrackedContentRecord record = newRecord();
        cache.put( record.getKey(), record );
        System.out.println("Put done");

        assertThat( cache.size(), equalTo( 1L ) );

        System.out.println("Invalidating all");
        cache.invalidateAll();
        System.out.println("All invalidated");

        System.out.println("Retrieving record");
        TrackedContentRecord retrieved = this.cache.getOrCreate( record.getKey() );
        System.out.println("Record retrieved");
        assertThat( retrieved, notNullValue() );

        System.out.println("Waiting for slow write to finish" );
        Thread.sleep( 600 );

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
        Cache<TrackingKey, TrackedContentRecord> cache = this.cache.buildCache();

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

        Cache<TrackingKey, TrackedContentRecord> recordCache = this.cache.buildCache( ( builder ) -> {
            builder.expireAfterAccess( 1, TimeUnit.MINUTES ).ticker( new Ticker()
            {
                @Override
                public long read()
                {
                    return ticker;
                }
            } );
        } );

        final TrackedContentRecord record = newRecord();
        recordCache.put( record.getKey(), record );

        assertThat( recordCache.size(), equalTo( 1L ) );

        ticker += 60 * 1000000000L + 1;
        assertThat( recordCache.get( record.getKey(), () -> {
            loadCalled = true;
            return record;
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

        final Cache<TrackingKey, TrackedContentRecord> recordCache = this.cache.buildCache( (builder)->{
                builder.expireAfterAccess( 1, TimeUnit.MINUTES ).ticker( new Ticker()
                {
                    @Override
                    public long read()
                    {
                        return ticker;
                    }
                } );
        } );

        final TrackedContentRecord record = newRecord();
        recordCache.put( record.getKey(), record );

        assertThat( recordCache.size(), equalTo( 1L ) );

        ticker += 60 * 1000000000L + 1;
        assertThat( this.cache.getOrCreate( record.getKey() ), notNullValue() );

        final File f = getFile( record );
        System.out.println( "Checking: " + f );
        assertThat( f.exists(), equalTo( true ) );
    }

    @Test
    public void writeRecordCreateCacheWithLoaderAndGetItem_VerifyRecordLoaded()
            throws Exception
    {
        this.cache.buildCache();
        final TrackedContentRecord record = newRecord();
        this.cache.write( record );

        final File f = getFile( record );
        System.out.println( "Checking: " + f );
        assertThat( f.exists(), equalTo( true ) );

        final Cache<TrackingKey, TrackedContentRecord> cache = CacheBuilder.newBuilder().build();

        final TrackedContentRecord result = cache.get( record.getKey(), ()->this.cache.load( record.getKey() ) );

        assertThat( result, notNullValue() );
        assertThat( result.getKey(), equalTo( record.getKey() ) );
        assertThat( cache.size(), equalTo( 1L ) );

    }

    @Test
    public void recordArtifactCreatesNewRecordIfNoneExist()
            throws Exception
    {
        this.cache.buildCache();
        final TrackingKey key = newKey();
        assertThat( cache.hasRecord( key ), equalTo( false ) );

        final TrackedContentRecord record =
                cache.recordArtifact( key, new StoreKey( StoreType.remote, "foo" ), "/path", StoreEffect.DOWNLOAD );

        assertThat( record, notNullValue() );
        assertThat( cache.hasRecord( key ), equalTo( true ) );
    }

    @Test
    public void clearRecordDeletesRecord()
            throws Exception
    {
        this.cache.buildCache();
        final TrackingKey key = newKey();
        assertThat( cache.hasRecord( key ), equalTo( false ) );

        final TrackedContentRecord record =
                cache.recordArtifact( key, new StoreKey( StoreType.remote, "foo" ), "/path", StoreEffect.DOWNLOAD );

        assertThat( record, notNullValue() );
        assertThat( cache.hasRecord( key ), equalTo( true ) );

        cache.delete( key );
        assertThat( cache.hasRecord( key ), equalTo( false ) );
        assertThat( cache.getIfExists( key ), nullValue() );
    }

    @Test
    public void getRecordReturnsNullIfNoneExists()
            throws Exception
    {
        this.cache.buildCache();
        final TrackingKey key = newKey();
        assertThat( cache.getIfExists( key ), nullValue() );
    }

    @Test
    public void hasRecordReturnsFalseIfNoneExists()
            throws Exception
    {
        this.cache.buildCache();
        final TrackingKey key = newKey();
        assertThat( cache.hasRecord( key ), equalTo( false ) );
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
