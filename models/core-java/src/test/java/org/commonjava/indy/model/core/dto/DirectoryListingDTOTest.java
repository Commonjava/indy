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

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
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
public class DirectoryListingDTOTest
{

    @Test
    public void jsonRoundTrip()
            throws IOException
    {
        DirectoryListingEntryDTO firstIn =
                new DirectoryListingEntryDTO( new StoreKey( StoreType.remote, "test" ), "/this/is/a/path.pom" );

        DirectoryListingEntryDTO secondIn =
                new DirectoryListingEntryDTO( new StoreKey( StoreType.remote, "test" ), "/this/is/another/path.pom" );

        DirectoryListingDTO in = new DirectoryListingDTO( Arrays.asList( firstIn, secondIn ) );

        IndyObjectMapper mapper = new IndyObjectMapper( true );
        String json = mapper.writeValueAsString( in );

        DirectoryListingDTO out = mapper.readValue( json, DirectoryListingDTO.class );

        assertThat( out, notNullValue() );

        List<DirectoryListingEntryDTO> items = out.getItems();
        assertThat( items.size(), equalTo( 2 ) );

        DirectoryListingEntryDTO firstOut = items.get( 0 );
        assertThat( firstOut.getKey(), equalTo( firstIn.getKey() ) );
        assertThat( firstOut.getPath(), equalTo( firstIn.getPath() ) );

        DirectoryListingEntryDTO secondOut = items.get( 1 );
        assertThat( secondOut.getKey(), equalTo( secondIn.getKey() ) );
        assertThat( secondOut.getPath(), equalTo( secondIn.getPath() ) );
    }
}
