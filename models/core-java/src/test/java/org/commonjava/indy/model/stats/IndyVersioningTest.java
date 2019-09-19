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
package org.commonjava.indy.model.stats;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.stats.IndyVersioning;
import org.junit.Test;

public class IndyVersioningTest
{

    @Test
    public void roundTripJson()
        throws Exception
    {
        final IndyObjectMapper mapper = new IndyObjectMapper( true );

        final IndyVersioning versioning =
            new IndyVersioning( "0.0.1", "somebody", "01010101010101", "2014-11-02 21:45:00", "1" );

        final String json = mapper.writeValueAsString( versioning );

        System.out.println( json );

        final IndyVersioning result = mapper.readValue( json, IndyVersioning.class );

        assertThat( result.getVersion(), equalTo( versioning.getVersion() ) );
        assertThat( result.getBuilder(), equalTo( versioning.getBuilder() ) );
        assertThat( result.getCommitId(), equalTo( versioning.getCommitId() ) );
        assertThat( result.getTimestamp(), equalTo( versioning.getTimestamp() ) );
    }

}
