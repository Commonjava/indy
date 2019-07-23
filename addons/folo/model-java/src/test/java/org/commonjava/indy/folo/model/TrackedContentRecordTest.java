/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.folo.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TrackedContentRecordTest
{
    private ObjectMapper mapper;

    @Before
    public void setup()
    {
        mapper = new IndyObjectMapper( false );
    }

    @Test
    public void roundTripEmptyRecordToJson()
        throws Exception
    {
        final TrackedContentRecord record = newRecord();
        final String json = mapper.writeValueAsString( record );
        System.out.println( json );

        final TrackedContentRecord result = mapper.readValue( json, TrackedContentRecord.class );

        assertThat( result, notNullValue() );
        assertThat( result.getKey(), equalTo( record.getKey() ) );
        assertThat( result.getAffectedStores(), nullValue() );
    }

    @Test
    public void createAffectedStoreRecordOnDemand()
    {
        final TrackedContentRecord record = newRecord();

        assertThat( record.getAffectedStores(), nullValue() );

        final StoreKey sk = new StoreKey( StoreType.hosted, "local" );
        final AffectedStoreRecord storeRecord = record.getAffectedStore( sk, true );

        assertThat( storeRecord, notNullValue() );

        final Map<StoreKey, AffectedStoreRecord> affectedStores = record.getAffectedStores();
        assertThat( affectedStores, notNullValue() );
        assertThat( affectedStores.size(), equalTo( 1 ) );
        assertThat( affectedStores.containsKey( sk ), equalTo( true ) );
    }

    @Test
    public void returnNullAffectedStoreRecordWhenCreateFlagIsFalse()
    {
        final TrackedContentRecord record = newRecord();

        assertThat( record.getAffectedStores(), nullValue() );

        final StoreKey sk = new StoreKey( StoreType.hosted, "local" );
        final AffectedStoreRecord storeRecord = record.getAffectedStore( sk, false );

        assertThat( storeRecord, nullValue() );

        final Map<StoreKey, AffectedStoreRecord> affectedStores = record.getAffectedStores();
        assertThat( affectedStores, nullValue() );
    }

    @Test
    public void returnNullAffectedStoreRecordWhenCreateFlagIsFalse_NonNullAffectedStoresMap()
    {
        final TrackedContentRecord record = newRecord();

        assertThat( record.getAffectedStores(), nullValue() );

        StoreKey sk = new StoreKey( StoreType.hosted, "local" );
        AffectedStoreRecord storeRecord = record.getAffectedStore( sk, true );

        assertThat( storeRecord, notNullValue() );

        final Map<StoreKey, AffectedStoreRecord> affectedStores = record.getAffectedStores();
        assertThat( affectedStores, notNullValue() );
        assertThat( affectedStores.size(), equalTo( 1 ) );
        assertThat( affectedStores.containsKey( sk ), equalTo( true ) );

        sk = new StoreKey( StoreType.hosted, "local2" );
        storeRecord = record.getAffectedStore( sk, false );

        assertThat( storeRecord, nullValue() );
    }

    private TrackedContentRecord newRecord()
    {
        final String id = "adsfadsfadsfadsfads";

        final TrackingKey key = new TrackingKey( id );

        return new TrackedContentRecord( key );
    }

}
