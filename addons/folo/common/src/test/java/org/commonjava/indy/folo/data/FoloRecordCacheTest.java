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

import org.commonjava.indy.folo.model.StoreEffect;
import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.folo.model.TrackedContentEntry;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class FoloRecordCacheTest
{

    private FoloRecordCache cache;

    private static Cache<TrackingKey, TrackedContent> sealed;

    private static EmbeddedCacheManager cacheManager;

    private static Cache<TrackedContentEntry, TrackedContentEntry> inProgress;

    //    @Rule
    //    public TemporaryFolder temp = new TemporaryFolder();

    @BeforeClass
    public static void setupClass()
    {
        cacheManager =
                new DefaultCacheManager( new ConfigurationBuilder().simpleCache( true ).build() );

        sealed = cacheManager.getCache( "sealed", true );
        inProgress = cacheManager.getCache( "in-progress", true );
    }

    @Before
    public void setup()
            throws Exception
    {
        cache = new FoloRecordCache( inProgress, sealed );
    }

    @After
    public void teardown()
    {
        inProgress.clear();
        sealed.clear();
    }

    @Test
    public void recordArtifactCreatesNewInProgressEntry()
            throws Exception
    {
        final TrackingKey key = newKey();
        assertThat( cache.hasRecord( key ), equalTo( false ) );

        cache.recordArtifact( new TrackedContentEntry( key, new StoreKey( StoreType.remote, "foo" ), "", "/path",
                                                       StoreEffect.DOWNLOAD, "", "", "" ) );

        assertThat( cache.hasRecord( key ), equalTo( true ) );
        assertThat( cache.hasInProgressRecord( key ), equalTo( true ) );
        assertThat( cache.hasSealedRecord( key ), equalTo( false ) );

        TrackedContent record = cache.seal( key );
        assertThat( record, notNullValue() );
        assertThat( cache.hasRecord( key ), equalTo( true ) );
        assertThat( cache.hasInProgressRecord( key ), equalTo( false ) );
        assertThat( cache.hasSealedRecord( key ), equalTo( true ) );
    }

    @Test
    public void sealRemovesInProgressAndCreatesSealedRecord()
            throws Exception
    {
        final TrackingKey key = newKey();
        assertThat( cache.hasRecord( key ), equalTo( false ) );

        cache.recordArtifact( new TrackedContentEntry( key, new StoreKey( StoreType.remote, "foo" ), "", "/path",
                                                       StoreEffect.DOWNLOAD, "", "", "" ) );

        assertThat( cache.hasRecord( key ), equalTo( true ) );
        assertThat( cache.hasInProgressRecord( key ), equalTo( true ) );
        assertThat( cache.hasSealedRecord( key ), equalTo( false ) );

        TrackedContent record = cache.seal( key );
        assertThat( record, notNullValue() );
        assertThat( cache.hasRecord( key ), equalTo( true ) );
        assertThat( cache.hasInProgressRecord( key ), equalTo( false ) );
        assertThat( cache.hasSealedRecord( key ), equalTo( true ) );
    }

    @Test
    public void clearRecordDeletesRecord()
            throws Exception
    {
        final TrackingKey key = newKey();
        assertThat( cache.hasRecord( key ), equalTo( false ) );

        cache.recordArtifact( new TrackedContentEntry( key, new StoreKey( StoreType.remote, "foo" ), "", "/path",
                                                       StoreEffect.DOWNLOAD, "", "", "" ) );

        TrackedContent record = cache.seal( key );

        assertThat( record, notNullValue() );
        assertThat( cache.hasRecord( key ), equalTo( true ) );

        cache.delete( key );
        assertThat( cache.hasRecord( key ), equalTo( false ) );
        assertThat( cache.get( key ), nullValue() );
    }

    @Test
    public void getRecordReturnsNullIfNoneExists()
            throws Exception
    {
        final TrackingKey key = newKey();
        assertThat( cache.get( key ), nullValue() );
    }

    @Test
    public void hasRecordReturnsFalseIfNoneExists()
            throws Exception
    {
        final TrackingKey key = newKey();
        assertThat( cache.hasRecord( key ), equalTo( false ) );
    }

    private TrackingKey newKey()
    {
        final String id = "track";

        return new TrackingKey( id );
    }

}
