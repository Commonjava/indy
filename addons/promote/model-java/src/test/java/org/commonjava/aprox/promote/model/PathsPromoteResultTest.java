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

public class PathsPromoteResultTest
{

    @Test
    public void roundTripJSON_CompletedNoPendingNoError()
        throws Exception
    {
        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final PathsPromoteResult in =
            new PathsPromoteResult( new PathsPromoteRequest( new StoreKey( StoreType.hosted, "source" ),
                                                   new StoreKey( StoreType.hosted, "target" ) ),
                               Collections.<String> emptySet(), new HashSet<String>( Arrays.asList( "/path/one",
                                                                                                    "/path/two" ) ),
                               null );

        final String json = mapper.writeValueAsString( in );

        System.out.println( json );

        final PathsPromoteResult out = mapper.readValue( json, PathsPromoteResult.class );

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

        final PathsPromoteResult in =
            new PathsPromoteResult( new PathsPromoteRequest( new StoreKey( StoreType.hosted, "source" ),
                                                   new StoreKey( StoreType.hosted, "target" ) ),
                               new HashSet<String>( Arrays.asList( "/path/one", "/path/two" ) ),
                               Collections.<String> emptySet(), "Something stupid happened" );

        final String json = mapper.writeValueAsString( in );

        System.out.println( json );

        final PathsPromoteResult out = mapper.readValue( json, PathsPromoteResult.class );

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

        final PathsPromoteResult in =
            new PathsPromoteResult( new PathsPromoteRequest( new StoreKey( StoreType.hosted, "source" ),
                                                   new StoreKey( StoreType.hosted, "target" ) ),
                               new HashSet<String>( Arrays.asList( "/path/one", "/path/two" ) ),
                               new HashSet<String>( Collections.singletonList( "/path/three" ) ), "Something stupid happened" );

        final String json = mapper.writeValueAsString( in );

        System.out.println( json );

        final PathsPromoteResult out = mapper.readValue( json, PathsPromoteResult.class );

        // we have separate unit tests to handle serialization checks for PromoteRequest...skipping here.
        assertThat( out.getPendingPaths(), equalTo( in.getPendingPaths() ) );
        assertThat( out.getCompletedPaths(), equalTo( in.getCompletedPaths() ) );
        assertThat( out.getError(), equalTo( in.getError() ) );
    }

}
