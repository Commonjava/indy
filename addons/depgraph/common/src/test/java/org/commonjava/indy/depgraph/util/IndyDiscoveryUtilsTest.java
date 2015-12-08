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
package org.commonjava.indy.depgraph.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.junit.Test;

public class IndyDiscoveryUtilsTest
{

    @Test
    public void parseTypeAndNameFromIndyURI()
        throws Exception
    {
        final URI uri = new URI( "indy:group:test" );
        final StoreKey key = IndyDepgraphUtils.getDiscoveryStore( uri );

        assertThat( key, notNullValue() );
        assertThat( key.getType(), equalTo( StoreType.group ) );
        assertThat( key.getName(), equalTo( "test" ) );
    }

}
