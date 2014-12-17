package org.commonjava.aprox.folo.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.folo.model.TrackingKey;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TrackingKeyTest
{

    private ObjectMapper mapper;

    @Before
    public void setup()
    {
        mapper = new AproxObjectMapper( false );
    }

    @Test
    public void roundTripToJson()
        throws Exception
    {
        final String id = "adsfadsfadsfadsfads";
        final StoreType type = StoreType.group;
        final String name = "test-group";

        final TrackingKey key = new TrackingKey( id, new StoreKey( type, name ) );

        final String json = mapper.writeValueAsString( key );
        System.out.println( json );

        final TrackingKey result = mapper.readValue( json, TrackingKey.class );

        assertThat( result, notNullValue() );
        assertThat( result, equalTo( key ) );
        assertThat( result.getId(), equalTo( key.getId() ) );
        assertThat( result.getTrackedStore(), equalTo( key.getTrackedStore() ) );
    }

    @Test( expected = NullPointerException.class )
    public void dontAllowNullTrackingId()
    {
        new TrackingKey( null, new StoreKey( StoreType.hosted, "foo" ) );
    }

    @Test( expected = NullPointerException.class )
    public void dontAllowNullTrackedStoreKeyId()
    {
        new TrackingKey( "foo", null );
    }

}
