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
package org.commonjava.indy.model.core.dto;

import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 4/25/16.
 */
public class EndpointViewListingTest
{
    @Test
    public void jsonRoundTrip()
            throws IOException
    {
        EndpointView first = new EndpointView( new RemoteRepository( "test", "http://foo.com/repo" ), "http://localhost:8080/api/remote/test" );
        EndpointView second = new EndpointView( new RemoteRepository( "test2", "http://foo2.com/repo2" ), "http://localhost:8080/api/remote/test2" );
        EndpointViewListing in = new EndpointViewListing( Arrays.asList( first, second ) );

        IndyObjectMapper mapper = new IndyObjectMapper( true );

        String json = mapper.writeValueAsString( in );

        EndpointViewListing out = mapper.readValue( json, EndpointViewListing.class );

        assertThat( out, notNullValue() );

        List<EndpointView> items = out.getItems();
        assertThat( items, notNullValue() );
        assertThat( items.size(), equalTo( 2 ) );

        int i=0;
        EndpointView view = items.get(i++);
        assertThat( view.getKey(), equalTo( first.getKey() ) );
        assertThat( view.getName(), equalTo( first.getName() ) );
        assertThat( view.getResourceUri(), equalTo( first.getResourceUri() ) );
        assertThat( view.getStoreKey(), equalTo( first.getStoreKey() ) );
        assertThat( view.getStoreType(), equalTo( first.getStoreType() ) );
        assertThat( view, equalTo( first ) );

        view = items.get(i++);
        assertThat( view.getKey(), equalTo( second.getKey() ) );
        assertThat( view.getName(), equalTo( second.getName() ) );
        assertThat( view.getResourceUri(), equalTo( second.getResourceUri() ) );
        assertThat( view.getStoreKey(), equalTo( second.getStoreKey() ) );
        assertThat( view.getStoreType(), equalTo( second.getStoreType() ) );
        assertThat( view, equalTo( second ) );

    }

}
