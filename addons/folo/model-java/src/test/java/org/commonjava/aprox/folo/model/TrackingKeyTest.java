package org.commonjava.aprox.folo.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

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
        final TrackingKey key = new TrackingKey( id );

        final String json = mapper.writeValueAsString( key );
        System.out.println( json );

        final TrackingKey result = mapper.readValue( json, TrackingKey.class );

        assertThat( result, notNullValue() );
        assertThat( result, equalTo( key ) );
        assertThat( result.getId(), equalTo( key.getId() ) );
    }

    @Test( expected = NullPointerException.class )
    public void dontAllowNullTrackingId()
    {
        new TrackingKey( null );
    }

}
