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

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class GroupPromoteResultTest
{

    @Test
    public void roundTripJSON_NoError()
        throws Exception
    {
        final IndyObjectMapper mapper = new IndyObjectMapper( true );

        final GroupPromoteResult in =
            new GroupPromoteResult( new GroupPromoteRequest( new StoreKey( StoreType.hosted, "source" ),
                                                   "target" ),
                               new ValidationResult() );

        final String json = mapper.writeValueAsString( in );

        System.out.println( json );

        final GroupPromoteResult out = mapper.readValue( json, GroupPromoteResult.class );

        // we have separate unit tests to handle serialization checks for PromoteRequest...skipping here.
        assertThat( out.getError(), equalTo( in.getError() ) );
    }

    @Test
    public void roundTripJSON_NoCompletedPendingWithError()
        throws Exception
    {
        final IndyObjectMapper mapper = new IndyObjectMapper( true );

        final GroupPromoteResult in =
            new GroupPromoteResult( new GroupPromoteRequest( new StoreKey( StoreType.hosted, "source" ),
                                                   "target" ),"Something stupid happened" );

        final String json = mapper.writeValueAsString( in );

        System.out.println( json );

        final GroupPromoteResult out = mapper.readValue( json, GroupPromoteResult.class );

        // we have separate unit tests to handle serialization checks for PromoteRequest...skipping here.
        assertThat( out.getError(), equalTo( in.getError() ) );
    }

}
