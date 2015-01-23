package org.commonjava.aprox.promote.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.junit.Test;

public class PromoteRequestTest
{

    @Test
    public void roundTripJSON_Basic()
        throws Exception
    {
        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final PromoteRequest req =
            new PromoteRequest( new StoreKey( StoreType.hosted, "source" ), new StoreKey( StoreType.hosted, "target" ) );

        final String json = mapper.writeValueAsString( req );

        System.out.println( json );

        final PromoteRequest result = mapper.readValue( json, PromoteRequest.class );

        assertThat( result.getSource(), equalTo( req.getSource() ) );
        assertThat( result.getTarget(), equalTo( req.getTarget() ) );
        assertThat( result.isPurgeSource(), equalTo( req.isPurgeSource() ) );
        assertThat( result.getPaths(), equalTo( req.getPaths() ) );
    }

    @Test
    public void roundTripJSON_withPaths()
        throws Exception
    {
        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final PromoteRequest req =
            new PromoteRequest( new StoreKey( StoreType.hosted, "source" ), new StoreKey( StoreType.hosted, "target" ),
                                new HashSet<String>( Arrays.asList( "/path/one", "/path/two" ) ) );

        final String json = mapper.writeValueAsString( req );

        System.out.println( json );

        final PromoteRequest result = mapper.readValue( json, PromoteRequest.class );

        assertThat( result.getSource(), equalTo( req.getSource() ) );
        assertThat( result.getTarget(), equalTo( req.getTarget() ) );
        assertThat( result.isPurgeSource(), equalTo( req.isPurgeSource() ) );
        assertThat( result.getPaths(), equalTo( req.getPaths() ) );
    }

    @Test
    public void roundTripJSON_withPurgeSource()
        throws Exception
    {
        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final PromoteRequest req =
            new PromoteRequest( new StoreKey( StoreType.hosted, "source" ), new StoreKey( StoreType.hosted, "target" ) ).setPurgeSource( true );

        final String json = mapper.writeValueAsString( req );

        System.out.println( json );

        final PromoteRequest result = mapper.readValue( json, PromoteRequest.class );

        assertThat( result.getSource(), equalTo( req.getSource() ) );
        assertThat( result.getTarget(), equalTo( req.getTarget() ) );
        assertThat( result.isPurgeSource(), equalTo( req.isPurgeSource() ) );
        assertThat( result.getPaths(), equalTo( req.getPaths() ) );
    }

}
