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
package org.commonjava.aprox.model.stats;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.stats.AProxVersioning;
import org.junit.Test;

public class AProxVersioningTest
{

    @Test
    public void roundTripJson()
        throws Exception
    {
        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final AProxVersioning versioning =
            new AProxVersioning( "0.0.1", "somebody", "01010101010101", "2014-11-02 21:45:00" );

        final String json = mapper.writeValueAsString( versioning );

        System.out.println( json );

        final AProxVersioning result = mapper.readValue( json, AProxVersioning.class );

        assertThat( result.getVersion(), equalTo( versioning.getVersion() ) );
        assertThat( result.getBuilder(), equalTo( versioning.getBuilder() ) );
        assertThat( result.getCommitId(), equalTo( versioning.getCommitId() ) );
        assertThat( result.getTimestamp(), equalTo( versioning.getTimestamp() ) );
    }

}
