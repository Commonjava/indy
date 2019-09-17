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
package org.commonjava.indy.core.expire;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.junit.Before;
import org.junit.Test;

public class ContentExpirationTest
{

    @Before
    public void setup()
    {
        System.out.println( System.getProperty( "java.class.path" )
                                  .replace( ':', '\n' ) );
        System.out.println();
    }

    @Test
    public void roundTrip()
        throws Exception
    {
        final ContentExpiration exp =
            new ContentExpiration( new StoreKey( StoreType.remote, "test" ), "/path/to/something.good" );
        final IndyObjectMapper mapper = new IndyObjectMapper( false );

        final String json = mapper.writeValueAsString( exp );
        System.out.println( json );

        final ContentExpiration result = mapper.readValue( json, ContentExpiration.class );

        assertThat( result, equalTo( exp ) );
    }

}
