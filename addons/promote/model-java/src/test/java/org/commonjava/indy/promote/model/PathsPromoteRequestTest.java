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
package org.commonjava.indy.promote.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashSet;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

public class PathsPromoteRequestTest
{

    @Test
    public void roundTripJSON_Basic()
        throws Exception
    {
        final IndyObjectMapper mapper = new IndyObjectMapper( true );

        final PathsPromoteRequest req =
            new PathsPromoteRequest( new StoreKey( StoreType.hosted, "source" ), new StoreKey( StoreType.hosted, "target" ) );

        final String json = mapper.writeValueAsString( req );

        System.out.println( json );

        final PathsPromoteRequest result = mapper.readValue( json, PathsPromoteRequest.class );

        assertThat( result.getSource(), equalTo( req.getSource() ) );
        assertThat( result.getTarget(), equalTo( req.getTarget() ) );
        assertThat( result.isPurgeSource(), equalTo( req.isPurgeSource() ) );
        assertThat( result.getPaths(), equalTo( req.getPaths() ) );
    }

    @Test
    public void roundTripJSON_withPaths()
        throws Exception
    {
        final IndyObjectMapper mapper = new IndyObjectMapper( true );

        final PathsPromoteRequest req =
            new PathsPromoteRequest( new StoreKey( StoreType.hosted, "source" ), new StoreKey( StoreType.hosted, "target" ),
                                new HashSet<String>( Arrays.asList( "/path/one", "/path/two" ) ) );

        final String json = mapper.writeValueAsString( req );

        System.out.println( json );

        final PathsPromoteRequest result = mapper.readValue( json, PathsPromoteRequest.class );

        assertThat( result.getSource(), equalTo( req.getSource() ) );
        assertThat( result.getTarget(), equalTo( req.getTarget() ) );
        assertThat( result.isPurgeSource(), equalTo( req.isPurgeSource() ) );
        assertThat( result.getPaths(), equalTo( req.getPaths() ) );
    }

    @Test
    public void roundTripJSON_withPurgeSource()
        throws Exception
    {
        final IndyObjectMapper mapper = new IndyObjectMapper( true );

        final PathsPromoteRequest req =
            new PathsPromoteRequest( new StoreKey( StoreType.hosted, "source" ), new StoreKey( StoreType.hosted, "target" ) ).setPurgeSource( true );

        final String json = mapper.writeValueAsString( req );

        System.out.println( json );

        final PathsPromoteRequest result = mapper.readValue( json, PathsPromoteRequest.class );

        assertThat( result.getSource(), equalTo( req.getSource() ) );
        assertThat( result.getTarget(), equalTo( req.getTarget() ) );
        assertThat( result.isPurgeSource(), equalTo( req.isPurgeSource() ) );
        assertThat( result.getPaths(), equalTo( req.getPaths() ) );
    }

}
