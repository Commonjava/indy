package org.commonjava.aprox.promote.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.junit.Test;

public class PromoteResultTest
{

    @Test
    public void roundTripJSON_CompletedNoPendingNoError()
        throws Exception
    {
        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final PromoteResult in =
            new PromoteResult( new PromoteRequest( new StoreKey( StoreType.hosted, "source" ),
                                                   new StoreKey( StoreType.hosted, "target" ) ),
                               Collections.<String> emptySet(), new HashSet<String>( Arrays.asList( "/path/one",
                                                                                                    "/path/two" ) ),
                               null );

        final String json = mapper.writeValueAsString( in );

        System.out.println( json );

        final PromoteResult out = mapper.readValue( json, PromoteResult.class );

        // we have separate unit tests to handle serialization checks for PromoteRequest...skipping here.
        assertThat( out.getPendingPaths(), equalTo( in.getPendingPaths() ) );
        assertThat( out.getCompletedPaths(), equalTo( in.getCompletedPaths() ) );
        assertThat( out.getError(), equalTo( in.getError() ) );
    }

    @Test
    public void roundTripJSON_NoCompletedPendingWithError()
        throws Exception
    {
        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final PromoteResult in =
            new PromoteResult( new PromoteRequest( new StoreKey( StoreType.hosted, "source" ),
                                                   new StoreKey( StoreType.hosted, "target" ) ),
                               new HashSet<String>( Arrays.asList( "/path/one", "/path/two" ) ),
                               Collections.<String> emptySet(), "Something stupid happened" );

        final String json = mapper.writeValueAsString( in );

        System.out.println( json );

        final PromoteResult out = mapper.readValue( json, PromoteResult.class );

        // we have separate unit tests to handle serialization checks for PromoteRequest...skipping here.
        assertThat( out.getPendingPaths(), equalTo( in.getPendingPaths() ) );
        assertThat( out.getCompletedPaths(), equalTo( in.getCompletedPaths() ) );
        assertThat( out.getError(), equalTo( in.getError() ) );
    }

    @Test
    public void roundTripJSON_CompletedANDPendingWithError()
        throws Exception
    {
        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final PromoteResult in =
            new PromoteResult( new PromoteRequest( new StoreKey( StoreType.hosted, "source" ),
                                                   new StoreKey( StoreType.hosted, "target" ) ),
                               new HashSet<String>( Arrays.asList( "/path/one", "/path/two" ) ),
                               new HashSet<String>( Arrays.asList( "/path/three" ) ), "Something stupid happened" );

        final String json = mapper.writeValueAsString( in );

        System.out.println( json );

        final PromoteResult out = mapper.readValue( json, PromoteResult.class );

        // we have separate unit tests to handle serialization checks for PromoteRequest...skipping here.
        assertThat( out.getPendingPaths(), equalTo( in.getPendingPaths() ) );
        assertThat( out.getCompletedPaths(), equalTo( in.getCompletedPaths() ) );
        assertThat( out.getError(), equalTo( in.getError() ) );
    }

}
