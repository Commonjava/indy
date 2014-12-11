package org.commonjava.aprox.folo.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.commonjava.aprox.folo.model.AffectedStoreRecord;
import org.commonjava.aprox.folo.model.TrackedContentRecord;
import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TrackedContentRecordTest
{
    private ObjectMapper mapper;

    @Before
    public void setup()
    {
        mapper = new AproxObjectMapper( false );
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
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final TrackingKey key = new TrackingKey( id, new StoreKey( type, name ) );

        return new TrackedContentRecord( key );
    }

}
